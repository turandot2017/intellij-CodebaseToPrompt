package com.github.codes2prompt.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PromptGeneratorDialog extends DialogWrapper {
    private final Project project;
    private final PsiFile[] psiFiles;
    private JPanel mainPanel;
    private JTextArea promptTextArea;
    private PromptToolbarPanel toolbarPanel;
    private FileTreePanel fileTreePanel;

    public PromptGeneratorDialog(Project project, PsiFile[] psiFiles) {
        super(project, true); // true means modal dialog
        this.project = project;
        this.psiFiles = psiFiles;
        
        init(); // 初始化对话框
        setTitle("codes2prompt - Prompt 内容生成"); // 设置窗口标题
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
        
        // 先设置文件树的回调
        fileTreePanel.setCallback(selectedFiles -> {
            // TODO: 更新 Prompt 内容
            // promptTextArea.setText(generatePrompt(selectedFiles));
        });
        
        // 再设置工具栏的回调
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
        // 创建底部状态栏（将在后续实现）
        JPanel statusPanel = new JBPanel<>(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(5));
        return statusPanel;
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
} 