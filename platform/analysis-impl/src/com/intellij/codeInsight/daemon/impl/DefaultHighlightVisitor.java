// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.AnnotatorStatisticsCollector;
import com.intellij.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class DefaultHighlightVisitor implements HighlightVisitor, DumbAware {
  private final Project myProject;
  private final boolean myHighlightErrorElements;
  private HighlightInfoHolder myHolder;
  private final boolean myBatchMode;
  private final AnnotatorStatisticsCollector myAnnotatorStatisticsCollector = new AnnotatorStatisticsCollector();

  @SuppressWarnings("UnusedDeclaration")
  DefaultHighlightVisitor(@NotNull Project project) {
    this(project, true, false);
  }

  DefaultHighlightVisitor(@NotNull Project project,
                          boolean highlightErrorElements,
                          boolean batchMode) {
    myProject = project;
    myHighlightErrorElements = highlightErrorElements;
    myBatchMode = batchMode;
  }

  @Override
  public boolean suitableForFile(@NotNull PsiFile file) {
    return true;
  }

  @Override
  public boolean analyze(@NotNull PsiFile file,
                         boolean updateWholeFile,
                         @NotNull HighlightInfoHolder holder,
                         @NotNull Runnable action) {
    myHolder = holder;

    try {
      action.run();
    }
    finally {
      myHolder = null;
    }
    return true;
  }

  @Override
  public void visit(@NotNull PsiElement element) {
    if (element instanceof PsiErrorElement && myHighlightErrorElements) {
      visitErrorElement((PsiErrorElement)element);
    }
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public @NotNull HighlightVisitor clone() {
    return new DefaultHighlightVisitor(myProject, myHighlightErrorElements, myBatchMode);
  }

  private void visitErrorElement(@NotNull PsiErrorElement element) {
    if (HighlightErrorFilter.EP_NAME.findFirstSafe(myProject, filter -> !filter.shouldHighlightErrorElement(element)) != null) {
      return;
    }

    myHolder.add(createErrorElementInfo(element));
  }

  private static HighlightInfo createErrorElementInfo(@NotNull PsiErrorElement element) {
    HighlightInfo.Builder builder = createErrorElementInfoWithoutFixes(element);
    List<ErrorQuickFixProvider> providers =
      DumbService.getInstance(element.getProject()).filterByDumbAwareness(ErrorQuickFixProvider.EP_NAME.getExtensionList());
    for (ErrorQuickFixProvider provider : providers) {
      provider.registerErrorQuickFix(element, builder);
    }
    builder.toolId(DefaultHighlightVisitor.class);
    HighlightInfo info = builder.create();
    if (info != null) {
      for (ErrorQuickFixProvider provider : providers) {
        provider.registerErrorQuickFix(element, info);
      }
    }
    return info;
  }

  private static @NotNull HighlightInfo.Builder createErrorElementInfoWithoutFixes(@NotNull PsiErrorElement element) {
    TextRange range = element.getTextRange();
    String errorDescription = element.getErrorDescription();
    if (!range.isEmpty()) {
      return HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(element).descriptionAndTooltip(errorDescription);
    }
    int offset = range.getStartOffset();
    PsiFile containingFile = element.getContainingFile();
    int fileLength = containingFile.getTextLength();
    FileViewProvider viewProvider = containingFile.getViewProvider();
    PsiElement elementAtOffset = viewProvider.findElementAt(offset, LanguageUtil.getRootLanguage(element));
    String text = elementAtOffset == null ? null : elementAtOffset.getText();
    if (offset < fileLength && text != null && !StringUtil.startsWithChar(text, '\n') && !StringUtil.startsWithChar(text, '\r')) {
      return HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(offset, offset + 1)
        .descriptionAndTooltip(errorDescription);
    }
    int start;
    int end;
    if (offset > 0) {
      start = offset/* - 1*/;
      end = offset;
    }
    else {
      start = offset;
      end = offset < fileLength ? offset + 1 : offset;
    }
    return HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(element, start, end)
    .descriptionAndTooltip(errorDescription)
    .endOfLine();
  }
}