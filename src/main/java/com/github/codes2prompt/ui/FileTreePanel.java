package com.github.codes2prompt.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

public class FileTreePanel extends JBPanel<FileTreePanel> {
    private final Project project;
    private final CheckboxTree tree;
    private final Map<String, CheckedTreeNode> nodeCache = new HashMap<>();
    private FileTreeCallback callback;

    public FileTreePanel(Project project, PsiFile[] psiFiles) {
        super(new BorderLayout());
        this.project = project;

        // 创建根节点
        CheckedTreeNode root = new CheckedTreeNode(null);
        
        // 创建树
        tree = new CheckboxTree(new FileTreeCellRenderer(), root) {
            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                // 节点状态改变时触发回调
                updateCallback();
            }
        };

        // 构建树结构
        buildTree(psiFiles);

        // 设置树的属性
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        // 添加到面板
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    private void buildTree(PsiFile[] psiFiles) {
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        nodeCache.clear();
        nodeCache.put("", root);

        // 按路径排序
        Arrays.sort(psiFiles, Comparator.comparing(file -> 
            file.getVirtualFile().getPath()));

        // 构建树结构
        for (PsiFile file : psiFiles) {
            VirtualFile vFile = file.getVirtualFile();
            String filePath = vFile.getPath();
            String relativePath = getRelativePath(filePath);
            
            // 创建文件节点的父目录节点
            createParentNodes(relativePath);
            
            // 创建文件节点
            CheckedTreeNode fileNode = new CheckedTreeNode(new FileTreeNode(file));
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
        public void customizeRenderer(JTree tree, Object value, boolean selected, 
                                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (!(value instanceof CheckedTreeNode)) return;
            
            CheckedTreeNode node = (CheckedTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof FileTreeNode) {
                FileTreeNode fileNode = (FileTreeNode) userObject;
                getTextRenderer().append(fileNode.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                getTextRenderer().setIcon(fileNode.getIcon());
            } else if (userObject instanceof FolderTreeNode) {
                FolderTreeNode folderNode = (FolderTreeNode) userObject;
                getTextRenderer().append(folderNode.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                getTextRenderer().setIcon(expanded ? 
                    AllIcons.Nodes.Folder : AllIcons.Nodes.Folder);
            }
        }
    }

    // 文件节点数据类
    private static class FileTreeNode {
        private final PsiFile file;

        public FileTreeNode(PsiFile file) {
            this.file = file;
        }

        public String getName() {
            return file.getName();
        }

        public Icon getIcon() {
            return file.getIcon(0);
        }

        public PsiFile getFile() {
            return file;
        }
    }

    // 文件夹节点数据类
    private static class FolderTreeNode {
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
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        collectSelectedFiles(root, selectedFiles);
        return selectedFiles;
    }

    private void collectSelectedFiles(CheckedTreeNode node, List<PsiFile> selectedFiles) {
        if (node.isLeaf() && node.isChecked()) {
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

    private void setNodesChecked(boolean checked) {
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        setNodeChecked(root, checked);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(root);
        updateCallback();
    }

    private void setNodeChecked(CheckedTreeNode node, boolean checked) {
        node.setChecked(checked);
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                setNodeChecked((CheckedTreeNode) node.getChildAt(i), checked);
            }
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
        if (callback != null) {
            callback.onSelectionChanged(getSelectedFiles());
        }
    }
} 