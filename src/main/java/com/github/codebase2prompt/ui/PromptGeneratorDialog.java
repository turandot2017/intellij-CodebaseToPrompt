package com.github.codebase2prompt.ui;

import com.github.codebase2prompt.core.PromptGenerator;
import com.github.codebase2prompt.core.TokenCounter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import com.github.codebase2prompt.storage.FileSelectionStorage;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import java.awt.*;

public class PromptGeneratorDialog extends DialogWrapper {
    private final Project project;
    private final PsiFile[] psiFiles;
    private JPanel mainPanel;
    private JTextArea promptTextArea;
    private PromptToolbarPanel toolbarPanel;
    private FileTreePanel fileTreePanel;
    private PromptGenerator promptGenerator;
    private JLabel statusLabel;

    public PromptGeneratorDialog(Project project, PsiFile[] psiFiles) {
        super(project, true); // true means modal dialog
        this.project = project;
        this.psiFiles = psiFiles;
        this.promptGenerator = new PromptGenerator(project);
        
        init(); // 初始化对话框
        setTitle("Codebase2Prompt - Prompt 内容生成"); // 设置窗口标题
        setSize(800, 600); // 设置窗口大小
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 创建主面板
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // 创建 Prompt 文本区域
        promptTextArea = new JTextArea();
        promptTextArea.setEditable(false);
        
        // 创建工具栏
        toolbarPanel = new PromptToolbarPanel(project, promptTextArea);
        // 创建文件树面板
        fileTreePanel = new FileTreePanel(project, psiFiles);
        
        // 建立联动关系
        fileTreePanel.setToolbarPanel(toolbarPanel);
        
        // 先设置文件树的回调
        fileTreePanel.setCallback(selectedFiles -> {
            String prompt = promptGenerator.generatePrompt(selectedFiles);
            promptTextArea.setText(prompt);
            promptTextArea.setCaretPosition(0); // 滚动到顶部
            
            // 更新状态栏
            updateStatusBar(selectedFiles.size(), TokenCounter.estimateTokens(prompt));
        });
        
        // 修改工具栏的回调实现
        toolbarPanel.setCallback(new PromptToolbarPanel.ToolbarCallback() {
            @Override
            public void onExpandAll() {
                fileTreePanel.expandAll();
            }
            
            @Override
            public void onCollapseAll() {
                fileTreePanel.collapseAll();
            }
            
            @Override
            public void onSelectAll() {
                fileTreePanel.selectAll();
            }
            
            @Override
            public void onUnselectAll() {
                fileTreePanel.unselectAll();
            }

            @Override
            public void onSaveSelection() {
                saveCurrentSelection();
            }

            @Override
            public void onLoadSelection(FileSelectionStorage.FileSelection selection) {
                fileTreePanel.loadSelection(selection.getFilePaths());
                Messages.showInfoMessage(project,
                    String.format("已加载选择：%s", selection.getName()),
                    "加载成功");
            }
        });
        
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        
        // 创建左侧面板
        fileTreePanel.setPreferredSize(new Dimension(300, -1));
        
        // 创建右侧面板
        JPanel rightPanel = new JBPanel<>(new BorderLayout());
        rightPanel.add(new JBScrollPane(promptTextArea), BorderLayout.CENTER);
        
        // 添加分隔面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, 
                fileTreePanel,
                new JBScrollPane(rightPanel));
        splitPane.setDividerLocation(300);
        
        // 将分隔面板添加到主面板
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        // 创建底部状态栏
        JPanel statusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(JBUI.Borders.empty(5));
        
        // 创建状态标签
        statusLabel = new JLabel();
        statusLabel.setFont(JBUI.Fonts.label());
        statusLabel.setBorder(JBUI.Borders.empty(3, 5));
        statusPanel.add(statusLabel);
        
        // 设置初始状态
        updateStatusBar(0, 0);
        
        return statusPanel;
    }

    /**
     * 更新状态栏显示
     * @param fileCount 选中的文件数量
     * @param tokenCount 估算的 Tokens 数量
     */
    private void updateStatusBar(int fileCount, int tokenCount) {
        StringBuilder status = new StringBuilder();
        status.append("文件数：").append(fileCount);
        status.append("  |  ");
        status.append("预计 Tokens 数量：").append(tokenCount);
        
        // 如果 Tokens 数量超过一定值，添加警告提示
        if (tokenCount > 32 * 1024) {
            status.append(" ⚠️ Tokens 数量较大，请注意拆分");
        }
        
        statusLabel.setText(status.toString());
    }

    @Override
    protected Action[] createActions() {
        // 只保留关闭按钮
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        // 处理确定按钮点击事件
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        // 处理取消按钮点击事件
        super.doCancelAction();
    }

    public void setSize(int width, int height) {
        getWindow().setMinimumSize(new Dimension(width, height));
        getWindow().setPreferredSize(new Dimension(width, height));
    }

    private void saveCurrentSelection() {
        // 获取当前选中的文件列表
        List<PsiFile> selectedFiles = fileTreePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            return;
        }

        // 创建并显示保存对话框
        SaveSelectionDialog dialog = new SaveSelectionDialog(project);
        if (dialog.showAndGet()) {
            // 获取用户输入
            String name = dialog.getSelectionName();
            String description = dialog.getSelectionDescription();

            // 转换文件列表为相对路径
            List<String> filePaths = selectedFiles.stream()
                .map(file -> {
                    VirtualFile vFile = file.getVirtualFile();
                    String fullPath = vFile.getPath();
                    String projectPath = project.getBasePath();
                    if (projectPath != null && fullPath.startsWith(projectPath)) {
                        return fullPath.substring(projectPath.length() + 1);
                    }
                    return fullPath;
                })
                .collect(Collectors.toList());

            // 保存选择
            FileSelectionStorage storage = FileSelectionStorage.getInstance(project);
            storage.saveSelection(name, description, filePaths);
            
            // 添加保存成功提示
            Messages.showInfoMessage(project,
                String.format("已保存选择：%s", name), 
                "保存成功");

            // // 打印保存的内容用于调试
            // System.out.println("Saved selections: " + storage.getAllSelections().size());
            // for (FileSelectionStorage.FileSelection sel : storage.getAllSelections()) {
            //     System.out.println("Name: " + sel.getName());
            //     System.out.println("Files: " + sel.getFilePaths());
            // }
        }
    }
} 