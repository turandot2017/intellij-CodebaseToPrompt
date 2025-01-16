# Codebase2Prompt

[English](README_EN.md) | 简体中文

## 简介

Codebase2Prompt 是一个 IntelliJ IDEA 插件，旨在帮助开发人员快速生成代码相关的 Prompt。它能够将搜索到的代码文件智能地组织成结构化的提示信息，方便在大型语言模型(LLM)中进行代码分析和问题咨询。

本工具参考 [CodebaseToPrompt](https://github.com/hello-nerdo/CodebaseToPrompt)  的功能，实现 Intellij 的插件版，方便在插件中使用。

![image-20250116123539684](http://yr-pic.yunrong.cn/md/202501161235101.png)



## 功能特点

- 🔍 与 IDEA 的 "Find in Files" 功能无缝集成
- 📁 交互式文件选择，支持目录树展示
- 🔄 实时生成格式化的 Prompt 内容
- 📊 自动估算 Tokens 使用量
- 📋 一键复制生成的 Prompt
- 🌳 支持文件夹结构可视化
- 🎯 支持批量文件选择和快速操作
- 💡 智能提示和友好的用户界面

## 安装说明

1. 下载插件包 (Codebase2Prompt-{version}.zip)
2. 在 IntelliJ IDEA 中:
   - 进入 `File` → `Settings` (Windows/Linux) 或 `IntelliJ IDEA` → `Preferences` (macOS)
   - 选择 `Plugins`
   - 点击 ⚙️ 图标，选择 `Install Plugin from Disk...`
   - 选择下载的插件包
   - 重启 IDE 完成安装

## 使用教程

### 基本使用流程

1. 在 IDEA 中使用 "Find in Files" 搜索相关代码
2. 点击搜索结果界面中的 "Codebase2Prompt" 按钮
3. 在弹出窗口中选择需要包含的文件
4. 点击复制按钮获取生成的 Prompt
5. 将 Prompt 粘贴到 LLM 对话窗口中使用

### 功能按钮说明

- **帮助**：查看详细使用说明
- **展开**：展开所有目录节点
- **折叠**：折叠所有目录节点
- **全选**：选择所有文件
- **全不选**：取消所有选择
- **复制**：复制生成的 Prompt 到剪贴板

### Prompt 格式说明

生成的 Prompt 包含两个主要部分：

1. 文件夹结构： 