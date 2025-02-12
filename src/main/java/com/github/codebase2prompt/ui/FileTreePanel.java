package com.github.codebase2prompt.ui;

import com.github.codebase2prompt.action.FindInFilesListener;
import com.github.codebase2prompt.util.PerformanceLogger;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.intellij.openapi.diagnostic.Logger;

public class FileTreePanel extends JBPanel<FileTreePanel> {
    private static final Logger LOG = Logger.getInstance(FileTreePanel.class);
    private final Project project;
    private final CheckboxTree tree;
    private final Map<String, CheckedTreeNode> nodeCache = new HashMap<>();
    private FileTreeCallback callback;
    private boolean isBatchUpdate = false;
    private PromptToolbarPanel toolbarPanel;

    // 添加延迟更新标记
    private boolean pendingCallbackUpdate = false;

    public FileTreePanel(Project project, PsiFile[] psiFiles) {
        super(new BorderLayout());
        this.project = project;

        // 创建根节点
        CheckedTreeNode root = new CheckedTreeNode(null);

        // 创建树
        tree = new CheckboxTree(new FileTreeCellRenderer(), root) {
            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof FolderTreeNode) {
                    handleDirectoryNodeStateChange(node, node.isChecked());
                } else if (userObject instanceof FileTreeNode) {
                    // 移除了 isBatchUpdate 标志操作，直接更新父节点
                    updateParentNodesState(node);
                    // 非批量操作时触发回调
                    if (!isBatchUpdate) {
                        updateCallback();
                        updateToolbarButtonState();
                    }
                } else if (!isBatchUpdate) {
                    updateCallback();
                    updateToolbarButtonState();
                }
            }
        };

        // 构建树结构
        buildTree(psiFiles);

        // 设置树的属性
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // 初始化时展开所有节点
        expandAll();
        // 初始化时取消所有选择
        unselectAll();

        // 添加到面板
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    private void buildTree(PsiFile[] psiFiles) {
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        nodeCache.clear();
        nodeCache.put("", root);

        // 按路径排序
        Arrays.sort(psiFiles, Comparator.comparing(file -> file.getVirtualFile()
            .getPath()));

        // 构建树结构
        for (PsiFile file : psiFiles) {
            VirtualFile vFile = file.getVirtualFile();
            String filePath = vFile.getPath();
            String relativePath = getRelativePath(filePath);

            // 创建文件节点的父目录节点
            createParentNodes(relativePath);

            // 检查文件大小
            boolean isLargeFile = vFile.getLength() > 100 * 1024; // 100KB
            if (isLargeFile) {
                LOG.warn("file " + filePath + "is too large, skip. size=" + vFile.getLength());
                // continue;
            }
            // 创建文件节点
            FileTreeNode fileTreeNode = new FileTreeNode(file, isLargeFile);
            CheckedTreeNode fileNode = new CheckedTreeNode(fileTreeNode);
            fileNode.setEnabled(!isLargeFile); // 大文件禁用选择

            String parentPath = getParentPath(relativePath);
            CheckedTreeNode parentNode = nodeCache.get(parentPath);
            parentNode.add(fileNode);
        }

        // 刷新树模型
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    private void createParentNodes(String filePath) {
        String parentPath = getParentPath(filePath);
        if (parentPath.isEmpty() || nodeCache.containsKey(parentPath)) {
            return;
        }

        // 递归创建父目录节点
        createParentNodes(parentPath);

        String folderName = getLastPathComponent(parentPath);
        CheckedTreeNode parentNode = new CheckedTreeNode(new FolderTreeNode(folderName));
        String grandParentPath = getParentPath(parentPath);
        CheckedTreeNode grandParentNode = nodeCache.get(grandParentPath);
        grandParentNode.add(parentNode);
        nodeCache.put(parentPath, parentNode);
    }

    // 自定义树节点渲染器
    private static class FileTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        @Override
        public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (!(value instanceof CheckedTreeNode))
                return;

            CheckedTreeNode node = (CheckedTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof FileTreeNode) {
                FileTreeNode fileNode = (FileTreeNode) userObject;
                getTextRenderer().append(fileNode.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                getTextRenderer().setIcon(fileNode.getIcon());
            } else if (userObject instanceof FolderTreeNode) {
                FolderTreeNode folderNode = (FolderTreeNode) userObject;
                getTextRenderer().append(folderNode.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                getTextRenderer().setIcon(expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder);
            }
        }
    }

    // 文件节点数据类
    public static class FileTreeNode {
        private final PsiFile file;
        private final boolean isLargeFile;

        public FileTreeNode(PsiFile file, boolean isLargeFile) {
            this.file = file;
            this.isLargeFile = isLargeFile;
        }

        public String getName() {
            return file.getName() + (isLargeFile ? " (too large)" : "");
        }

        public Icon getIcon() {
            return file.getIcon(0);
        }

        public PsiFile getFile() {
            return file;
        }

        public boolean isLargeFile() {
            return isLargeFile;
        }
    }

    // 文件夹节点数据类
    public static class FolderTreeNode {
        private final String name;

        public FolderTreeNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // 工具方法
    private String getRelativePath(String fullPath) {
        String projectPath = project.getBasePath();
        if (projectPath != null && fullPath.startsWith(projectPath)) {
            return fullPath.substring(projectPath.length() + 1);
        }
        return fullPath;
    }

    private String getParentPath(String path) {
        int lastSeparator = path.lastIndexOf('/');
        return lastSeparator > 0 ? path.substring(0, lastSeparator) : "";
    }

    private String getLastPathComponent(String path) {
        int lastSeparator = path.lastIndexOf('/');
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }

    // 获取选中的文件列表
    public List<PsiFile> getSelectedFiles() {
        List<PsiFile> selectedFiles = new ArrayList<>();
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel()
            .getRoot();
        collectSelectedFiles(root, selectedFiles);
        return selectedFiles;
    }

    private void collectSelectedFiles(CheckedTreeNode node, List<PsiFile> selectedFiles) {
        PerformanceLogger.traceNode(node, node.isChecked(), "collectSelectedFiles ...");
        if (node.isLeaf() && node.isChecked()) {
            if (!node.isEnabled() && node.isChecked()) {
                node.setChecked(false);
                PerformanceLogger.traceNode(node, true, "collectSelectedFiles:数据不正确!");
                return;
            }
            Object userObject = node.getUserObject();
            if (userObject instanceof FileTreeNode) {
                selectedFiles.add(((FileTreeNode) userObject).getFile());
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                collectSelectedFiles((CheckedTreeNode) node.getChildAt(i), selectedFiles);
            }
        }
    }

    // 展开/折叠方法
    public void expandAll() {
        TreeUtil.expandAll(tree);
    }

    public void collapseAll() {
        TreeUtil.collapseAll(tree, 1);
    }

    // 全选/全不选方法
    public void selectAll() {
        setNodesChecked(true);
    }

    public void unselectAll() {
        setNodesChecked(false);
    }

    // 在 setNodesChecked 方法中添加延迟合并
    private void setNodesChecked(boolean checked) {
        SwingUtilities.invokeLater(() -> {
            try {
                isBatchUpdate = true;
                pendingCallbackUpdate = true;
                CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
                setNodeChecked(root, checked);
                updateAllNodesState(root);
                ((DefaultTreeModel) tree.getModel()).nodeChanged(root);
            } finally {
                isBatchUpdate = false;
                if (pendingCallbackUpdate) {
                    pendingCallbackUpdate = false;
                    updateCallback();
                    updateToolbarButtonState();
                }
            }
        });
    }

    private void setNodeChecked(CheckedTreeNode node, boolean checked) {
        if (!node.isLeaf()) {
            PerformanceLogger.traceNode(node, true);
            node.setEnabled(true);
        } else {
            if (!node.isEnabled()) {
                PerformanceLogger.traceNode(node, false);
                node.setChecked(false);
            } else {
                PerformanceLogger.traceNode(node, checked);
                node.setChecked(checked);
            }
        }

        // 只处理子节点，不处理父节点
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                setNodeChecked((CheckedTreeNode) node.getChildAt(i), checked);
            }
        }
    }

    // 增强批量操作处理逻辑
    private void handleDirectoryNodeStateChange(CheckedTreeNode node, boolean checked) {
        try {
            isBatchUpdate = true;
            pendingCallbackUpdate = true;

            // 使用 SwingUtilities 合并 UI 更新
            SwingUtilities.invokeLater(() -> {
                try {
                    setNodeChecked(node, checked);
                    updateParentNodesState(node);
                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
                } finally {
                    isBatchUpdate = false;
                    if (pendingCallbackUpdate) {
                        pendingCallbackUpdate = false;
                        updateCallback();
                        updateToolbarButtonState();
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Directory node state change failed", e);
        }
    }

    // 添加新方法：更新父节点状态
    private void updateParentNodesState(CheckedTreeNode node) {
        CheckedTreeNode parent = (CheckedTreeNode) node.getParent();
        while (parent != null && parent != tree.getModel()
            .getRoot()) {
            updateNodeCheckState(parent);
            ((DefaultTreeModel) tree.getModel()).nodeChanged(parent);
            parent = (CheckedTreeNode) parent.getParent();
        }
    }

    // 修改更新节点的选中状态的方法
    private void updateNodeCheckState(CheckedTreeNode node) {
        int childCount = node.getChildCount();
        if (childCount == 0) {
            return;
        }

        int checkedCount = 0;
        for (int i = 0; i < childCount; i++) {
            CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            if (child.isChecked()) {
                checkedCount++;
            }
        }

        // 更新节点状态
        if (checkedCount == childCount) {
            if (node.isEnabled()) {
                PerformanceLogger.traceNode(node, true);
                node.setChecked(true);
            } else {
                PerformanceLogger.traceNode(node, false);
                node.setChecked(false);
            }
        } else {
            PerformanceLogger.traceNode(node, false);
            node.setChecked(false);
        }
        if (!node.isLeaf()) {
            node.setEnabled(true);  // 保持节点始终可用
        }
    }

    // 添加新方法：更新所有节点状态
    private void updateAllNodesState(CheckedTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            updateNodeCheckState(child);
            updateAllNodesState(child);
        }
    }

    // 回调接口
    public interface FileTreeCallback {
        void onSelectionChanged(List<PsiFile> selectedFiles);
    }

    public void setCallback(FileTreeCallback callback) {
        this.callback = callback;
    }

    private void updateCallback() {
        if (!isBatchUpdate && callback != null) {
            callback.onSelectionChanged(getSelectedFiles());
        }
    }

    public void setToolbarPanel(PromptToolbarPanel toolbarPanel) {
        this.toolbarPanel = toolbarPanel;
        updateToolbarButtonState();
    }

    private void updateToolbarButtonState() {
        if (toolbarPanel != null) {
            List<PsiFile> selectedFiles = getSelectedFiles();
            toolbarPanel.updateFileSelectionState(!selectedFiles.isEmpty());
        }
    }

    // 新增：加载选择记录的方法
    public static class LoadSelectionResult {
        private final int totalFiles;      // 历史选择中的文件总数
        private final int loadedFiles;     // 成功加载的文件数
        private final List<String> missingFiles; // 未找到的文件列表

        public LoadSelectionResult(int totalFiles, int loadedFiles, List<String> missingFiles) {
            this.totalFiles = totalFiles;
            this.loadedFiles = loadedFiles;
            this.missingFiles = missingFiles;
        }

        public int getTotalFiles() {return totalFiles;}

        public int getLoadedFiles() {return loadedFiles;}

        public List<String> getMissingFiles() {return missingFiles;}
    }

    public LoadSelectionResult loadSelection(List<String> filePaths) {
        try {
            isBatchUpdate = true;
            pendingCallbackUpdate = true;

            // 先取消所有选择
            unselectAll();

            // 获取项目根路径
            String projectPath = project.getBasePath();
            if (projectPath == null) {
                return new LoadSelectionResult(filePaths.size(), 0, new ArrayList<>(filePaths));
            }

            List<String> missingFiles = new ArrayList<>();
            int loadedCount = 0;
            Set<CheckedTreeNode> parentsToExpand = new HashSet<>();

            // 选中指定的文件
            CheckedTreeNode root = (CheckedTreeNode) tree.getModel()
                .getRoot();
            for (String relativePath : filePaths) {
                // 转换为完整路径
                String fullPath = projectPath + "/" + relativePath;
                if (selectNodeByPath(root, fullPath, parentsToExpand)) {
                    loadedCount++;
                } else {
                    missingFiles.add(relativePath);
                }
            }

            // 更新树和按钮状态
            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(root);

            // 展开所有已选中文件的父节点
            for (CheckedTreeNode node : parentsToExpand) {
                TreePath path = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node));
                tree.expandPath(path);
            }

            return new LoadSelectionResult(filePaths.size(), loadedCount, missingFiles);
        } finally {
            isBatchUpdate = false;
            if (pendingCallbackUpdate) {
                pendingCallbackUpdate = false;
                updateCallback();
                updateToolbarButtonState();
            }
        }
    }

    private boolean selectNodeByPath(CheckedTreeNode node, String targetPath, Set<CheckedTreeNode> parentsToExpand) {
        Object userObject = node.getUserObject();
        if (userObject instanceof FileTreeNode) {
            FileTreeNode fileNode = (FileTreeNode) userObject;
            if (fileNode.getFile()
                .getVirtualFile()
                .getPath()
                .equals(targetPath)) {
                if (node.isEnabled()) {
                    node.setChecked(true);
                } else {
                    node.setChecked(false);
                }
                // 记录所有父节点，以便后续展开
                CheckedTreeNode parent = (CheckedTreeNode) node.getParent();
                while (parent != null && parent != tree.getModel()
                    .getRoot()) {
                    parentsToExpand.add(parent);
                    parent = (CheckedTreeNode) parent.getParent();
                }
                updateParentNodesState(node);
                return true;
            }
        }

        // 递归搜索子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                if (selectNodeByPath((CheckedTreeNode) node.getChildAt(i), targetPath, parentsToExpand)) {
                    return true;
                }
            }
        }
        return false;
    }
} 