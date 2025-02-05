// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.changes.issueLinks;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;


public class TreeLinkMouseListener extends LinkMouseListenerBase<Object> {
  private final ColoredTreeCellRenderer myRenderer;
  private WeakReference<Object> myLastHitNode;

  public TreeLinkMouseListener(final ColoredTreeCellRenderer renderer) {
    myRenderer = renderer;
  }

  protected void showTooltip(final JTree tree, final MouseEvent e, final HaveTooltip launcher) {
    final String text = tree.getToolTipText(e);
    final String newText = launcher == null ? null : launcher.getTooltip();
    if (!Objects.equals(text, newText)) {
      tree.setToolTipText(newText);
    }
  }

  @Override
  protected @Nullable Object getTagAt(final @NotNull MouseEvent e) {
    JTree tree = (JTree)e.getSource();
    Object tag = null;
    HaveTooltip haveTooltip = null;
    final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
    if (path != null) {
      int dx = getRendererRelativeX(e, tree, path);
      final Object node = path.getLastPathComponent();

      boolean isLeaf;
      if (node instanceof TreeNode treeNode) {
        isLeaf = treeNode.isLeaf();
      } else if (node instanceof IsLeafProvider isLeafProvider) {
        isLeaf = isLeafProvider.isLeaf();
      } else {
        isLeaf = false;
      }
      AppUIUtil.targetToDevice(myRenderer, tree);
      if (myLastHitNode == null || myLastHitNode.get() != node || e.getButton() != MouseEvent.NOBUTTON) {
        if (doCacheLastNode()) {
          myLastHitNode = new WeakReference<>(node);
        }
        myRenderer.getTreeCellRendererComponent(tree, node, false, false, isLeaf, tree.getRowForPath(path), false);
      }
      tag = myRenderer.getFragmentTagAt(dx);
      if (tag != null && node instanceof HaveTooltip) {
        haveTooltip = (HaveTooltip)node;
      }
    }
    showTooltip(tree, e, haveTooltip);
    return tag;
  }

  protected int getRendererRelativeX(@NotNull MouseEvent e, @NotNull JTree tree, @NotNull TreePath path) {
    final Rectangle rectangle = tree.getPathBounds(path);
    assert rectangle != null;
    return e.getX() - rectangle.x;
  }

  protected boolean doCacheLastNode() {
    return true;
  }

  public interface HaveTooltip {
    @NlsContexts.Tooltip String getTooltip();
  }

  @ApiStatus.Internal
  public interface IsLeafProvider {
    boolean isLeaf();
  }
}
