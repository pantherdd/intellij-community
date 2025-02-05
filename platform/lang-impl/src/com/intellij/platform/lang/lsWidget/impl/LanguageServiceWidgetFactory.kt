// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.lang.lsWidget.impl

import com.intellij.lang.LangBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory
import kotlinx.coroutines.CoroutineScope

class LanguageServiceWidgetFactory : StatusBarEditorBasedWidgetFactory() {
  companion object {
    const val ID = "LanguageServiceStatusBarWidget"
  }

  override fun getId(): String = ID

  override fun getDisplayName(): String = LangBundle.message("language.services.widget")

  override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget = LanguageServiceWidget(project, scope)
}
