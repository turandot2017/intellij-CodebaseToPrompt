package com.github.codebase2prompt.ui;

import com.github.codebase2prompt.storage.FileSelectionStorage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SaveSelectionDialog extends DialogWrapper {
    private final Project project;
    private final FileSelectionStorage storage;
    private JBTextField nameField;
    private JTextArea descriptionArea;
    private boolean isOverwrite = false;
    private boolean userCancelled = false;

    public SaveSelectionDialog(Project project) {
        super(project);
        this.project = project;
        this.storage = FileSelectionStorage.getInstance(project);
        
        init();
        setTitle("保存当前选择");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 创建主面板
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        GridBagConstraints c = new GridBagConstraints();
        
        // 名称输入框
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = JBUI.insets(0, 0, 5, 5);
        panel.add(new JBLabel("名称："), c);
        
        nameField = new JBTextField();
        nameField.setPreferredSize(new Dimension(300, 30));
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        panel.add(nameField, c);
        
        // 描述输入区域
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = JBUI.insets(10, 0, 5, 5);
        panel.add(new JBLabel("描述："), c);
        
        descriptionArea = new JTextArea(3, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JBScrollPane scrollPane = new JBScrollPane(descriptionArea);
        c.gridx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panel.add(scrollPane, c);
        
        return panel;
    }

    @Override
    protected ValidationInfo doValidate() {
        // 重置状态
        if (nameField.getText().trim().isEmpty()) {
            userCancelled = false;
            return new ValidationInfo("请输入名称", nameField);
        }
        
        // 检查名称是否重复，如果重复且未确认覆盖，则提示用户
        if (!isOverwrite && !userCancelled && storage.isNameExists(nameField.getText().trim())) {
            int result = Messages.showYesNoDialog(
                project,
                String.format("选择 %s 已存在，是否覆盖？", nameField.getText().trim()),
                "确认覆盖",
                "覆盖",
                "取消",
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                isOverwrite = true;
                userCancelled = false;
                return null;
            } else {
                userCancelled = true;
                return new ValidationInfo("请使用其他名称", nameField);
            }
        }
        
        // 如果用户修改了名称，重置状态
        if (!storage.isNameExists(nameField.getText().trim())) {
            isOverwrite = false;
            userCancelled = false;
        }
        
        return null;
    }

    public String getSelectionName() {
        return nameField.getText().trim();
    }

    public String getSelectionDescription() {
        return descriptionArea.getText().trim();
    }

    public boolean isOverwriteMode() {
        return isOverwrite;
    }
} 