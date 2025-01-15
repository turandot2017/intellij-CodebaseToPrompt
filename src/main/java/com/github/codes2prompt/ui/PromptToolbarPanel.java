package com.github.codes2prompt.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

public class PromptToolbarPanel extends JBPanel<PromptToolbarPanel> {
    private final Project project;
    private final JTextArea promptTextArea; // 用于获取和设置 Prompt 内容

    public PromptToolbarPanel(Project project, JTextArea promptTextArea) {
        super(new BorderLayout());
        this.project = project;
        this.promptTextArea = promptTextArea;
        initToolbar();
    }

    private void initToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // 左侧按钮组
        DefaultActionGroup leftGroup = new DefaultActionGroup();
        
        // 帮助按钮
        leftGroup.add(new AnAction("帮助", "查看插件使用说明", AllIcons.Actions.Help) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showHelpDialog();
            }
        });

        // 分隔符
        leftGroup.add(Separator.getInstance());

        // 展开按钮
        leftGroup.add(new AnAction("展开", "展开所有目录", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onExpandAll();
                }
            }
        });

        // 折叠按钮
        leftGroup.add(new AnAction("折叠", "折叠所有目录", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onCollapseAll();
                }
            }
        });

        // 分隔符
        leftGroup.add(Separator.getInstance());

        // 全选按钮
        leftGroup.add(new AnAction("全选", "选择所有文件", AllIcons.Actions.Selectall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onSelectAll();
                }
            }
        });

        // 全不选按钮
        leftGroup.add(new AnAction("全不选", "取消选择所有文件", AllIcons.Actions.Unselectall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onUnselectAll();
                }
            }
        });

        // 添加左侧按钮组
        actionGroup.add(leftGroup);
        
        // 添加右对齐分隔符
        actionGroup.addSeparator();
        
        // 复制按钮
        DefaultActionGroup rightGroup = new DefaultActionGroup();
        AnAction copyAction = new AnAction("复制", "复制到剪贴板", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyToClipboard();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                // 根据是否有内容来启用/禁用复制按钮
                e.getPresentation().setEnabled(promptTextArea != null && 
                    !promptTextArea.getText().trim().isEmpty());
                // 设置为主按钮样式
                e.getPresentation().setText("复制");
                e.getPresentation().setIcon(AllIcons.Actions.Copy);
                // e.getPresentation().setPrimaryAction(true);
            }
        };
        rightGroup.add(copyAction);
        actionGroup.add(rightGroup);

        // 创建工具栏
        ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true);
        toolbar.setTargetComponent(this);
        JPanel toolbarPanel = new JBPanel<>(new BorderLayout());
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
        toolbarPanel.setBorder(JBUI.Borders.empty(2, 2));
        
        // 添加工具栏到面板
        add(toolbarPanel, BorderLayout.CENTER);
    }

    private void showHelpDialog() {
        String helpContent = 
            "codes2prompt 插件使用说明\n\n" +
            "1. 在左侧目录树中选择需要包含的文件\n" +
            "2. 使用工具栏按钮进行操作：\n" +
            "   - 展开：展开所有目录\n" +
            "   - 折叠：折叠所有目录\n" +
            "   - 全选：选择所有文件\n" +
            "   - 全不选：取消选择所有文件\n" +
            "   - 复制：将生成的 Prompt 复制到剪贴板\n" +
            "3. 右侧预览区域显示生成的 Prompt 内容\n" +
            "4. 底部显示已选择的文件数量和预计 Tokens\n\n" +
            "版本：1.0.0\n" +
            "作者：Your Name";

        Messages.showInfoMessage(
            project,
            helpContent,
            "codes2prompt 帮助"
        );
    }

    private void copyToClipboard() {
        String content = promptTextArea.getText();
        if (content != null && !content.trim().isEmpty()) {
            StringSelection selection = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            Messages.showInfoMessage(project, "内容已复制到剪贴板", "复制成功");
        }
    }

    // 用于设置回调接口
    public interface ToolbarCallback {
        void onExpandAll();
        void onCollapseAll();
        void onSelectAll();
        void onUnselectAll();
    }

    private ToolbarCallback callback;

    public void setCallback(ToolbarCallback callback) {
        this.callback = callback;
    }
} 