package com.github.codebase2prompt.util;

import com.github.codebase2prompt.ui.FileTreePanel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.CheckedTreeNode;

public class PerformanceLogger {
    private static final Logger LOG = Logger.getInstance(PerformanceLogger.class);

    public static void logTime(String operation, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        LOG.info(String.format("%s took %d ms", operation, elapsed));
    }

    public static void traceNode(CheckedTreeNode node, boolean checked){
        // if (node.getUserObject() instanceof FileTreePanel.FileTreeNode ){
        //     FileTreePanel.FileTreeNode fileTreeNode = (FileTreePanel.FileTreeNode) node.getUserObject();
        //     LOG.info(String.format("Trace file node: %s is large %s, checked: %s", fileTreeNode.getName(), fileTreeNode.isLargeFile(), checked));
        // } else if (node.getUserObject() instanceof FileTreePanel.FolderTreeNode ){
        //     FileTreePanel.FolderTreeNode folderTreeNode = (FileTreePanel.FolderTreeNode) node.getUserObject();
        //     LOG.info(String.format("Trace folder node: %s is large %s, checked: %s", folderTreeNode.getName(), false, checked));
        // } else {
        //     LOG.warn(String.format("Trace node: unknown type %s", node.toString()));
        // }
    }

    public static void traceNode(CheckedTreeNode node, boolean checked, String info) {
        // traceNode(node, checked);
        // LOG.warn(info);
    }
} 