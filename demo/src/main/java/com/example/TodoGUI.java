package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoGUI extends JFrame {
    // 日志记录器，帮助我们追踪程序运行状态
    private static final Logger logger = LoggerFactory.getLogger(TodoGUI.class);
    
    // 数据访问对象，处理与数据库的交互
    private final TodoDao todoDao;
    
    // GUI组件
    private JTextField contentField;
    private JComboBox<Priority> priorityComboBox;
    private JList<Todo> todoList;
    private DefaultListModel<Todo> listModel;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    
    // 日期时间格式化器
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TodoGUI() {
        // 初始化数据访问对象
        todoDao = new TodoDao();
        
        // 设置窗口基本属性
        setTitle("待办事项管理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout(10, 10));
        
        // 初始化所有GUI组件
        initializeComponents();
        
        // 加载现有的待办事项
        refreshTodoList();
        
        // 设置窗口在屏幕中央显示
        setLocationRelativeTo(null);
        
        // 注册窗口关闭事件，确保正确清理资源
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                todoDao.close();
                logger.info("应用程序正常关闭");
            }
        });
    }

    private void initializeComponents() {
        // 创建输入面板
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);
        
        // 创建列表面板
        JPanel listPanel = createListPanel();
        add(listPanel, BorderLayout.CENTER);
        
        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 内容输入框
        JLabel contentLabel = new JLabel("内容:");
        contentField = new JTextField(20);
        
        // 优先级选择框
        JLabel priorityLabel = new JLabel("优先级:");
        priorityComboBox = new JComboBox<>(Priority.values());

        // 使用GridBagLayout排列组件
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(contentLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(contentField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(priorityLabel, gbc);

        gbc.gridx = 3;
        panel.add(priorityComboBox, gbc);

        return panel;
    }

    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建带有自定义渲染器的待办事项列表
        listModel = new DefaultListModel<>();
        todoList = new JList<>(listModel);
        todoList.setCellRenderer(new TodoListCellRenderer());
        todoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 添加列表选择监听器
        todoList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Todo selected = todoList.getSelectedValue();
                if (selected != null) {
                    contentField.setText(selected.getContent());
                    priorityComboBox.setSelectedItem(selected.getPriority());
                    updateButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                }
            }
        });

        // 将列表放入滚动面板
        JScrollPane scrollPane = new JScrollPane(todoList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // 创建按钮
        addButton = new JButton("添加");
        updateButton = new JButton("更新");
        deleteButton = new JButton("删除");
        
        // 初始状态下禁用更新和删除按钮
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // 添加按钮事件监听器
        addButton.addActionListener(e -> addTodo());
        updateButton.addActionListener(e -> updateTodo());
        deleteButton.addActionListener(e -> deleteTodo());

        // 添加按钮到面板
        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);

        return panel;
    }

    private void addTodo() {
        try {
            String content = contentField.getText().trim();
            if (content.isEmpty()) {
                showError("请输入待办事项内容");
                return;
            }

            Priority priority = (Priority) priorityComboBox.getSelectedItem();
            Todo todo = new Todo(content, LocalDateTime.now(), priority);
            
            todoDao.addTodo(todo);
            refreshTodoList();
            clearInputs();
            
            logger.info("成功添加待办事项: {}", content);
        } catch (Exception e) {
            logger.error("添加待办事项失败", e);
            showError("添加待办事项失败: " + e.getMessage());
        }
    }

    private void updateTodo() {
        try {
            Todo selected = todoList.getSelectedValue();
            if (selected == null) {
                showError("请选择要更新的待办事项");
                return;
            }

            String content = contentField.getText().trim();
            if (content.isEmpty()) {
                showError("请输入待办事项内容");
                return;
            }

            Priority priority = (Priority) priorityComboBox.getSelectedItem();
            Todo updated = new Todo(selected.getId(), content, LocalDateTime.now(), priority);
            
            todoDao.updateTodo(updated);
            refreshTodoList();
            clearInputs();
            
            logger.info("成功更新待办事项: ID={}", selected.getId());
        } catch (Exception e) {
            logger.error("更新待办事项失败", e);
            showError("更新待办事项失败: " + e.getMessage());
        }
    }

    private void deleteTodo() {
        try {
            Todo selected = todoList.getSelectedValue();
            if (selected == null) {
                showError("请选择要删除的待办事项");
                return;
            }

            int option = JOptionPane.showConfirmDialog(
                this,
                "确定要删除这个待办事项吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
            );

            if (option == JOptionPane.YES_OPTION) {
                todoDao.deleteTodo(selected.getId());
                refreshTodoList();
                clearInputs();
                logger.info("成功删除待办事项: ID={}", selected.getId());
            }
        } catch (Exception e) {
            logger.error("删除待办事项失败", e);
            showError("删除待办事项失败: " + e.getMessage());
        }
    }

    private void refreshTodoList() {
        try {
            listModel.clear();
            todoDao.getAllTodos().forEach(listModel::addElement);
        } catch (Exception e) {
            logger.error("刷新待办事项列表失败", e);
            showError("刷新列表失败: " + e.getMessage());
        }
    }

    private void clearInputs() {
        contentField.setText("");
        priorityComboBox.setSelectedIndex(0);
        todoList.clearSelection();
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "错误",
            JOptionPane.ERROR_MESSAGE
        );
    }

    // 自定义列表单元渲染器
    private class TodoListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Todo) {
                Todo todo = (Todo) value;
                String text = String.format("[%s] %s - %s",
                    todo.getPriority(),
                    todo.getContent(),
                    todo.getDateTime().format(FORMATTER)
                );
                setText(text);
                
                // 根据优先级设置不同的背景色
                if (!isSelected) {
                    switch (todo.getPriority()) {
                        case HIGH:
                            setBackground(new Color(255, 200, 200));
                            break;
                        case MEDIUM:
                            setBackground(new Color(255, 255, 200));
                            break;
                        case LOW:
                            setBackground(new Color(200, 255, 200));
                            break;
                    }
                }
            }
            return this;
        }
    }

    public static void main(String[] args) {
        // 使用事件调度线程创建和显示GUI
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
                new TodoGUI().setVisible(true);
                logger.info("待办事项管理器启动成功");
            } catch (Exception e) {
                logger.error("启动应用程序失败", e);
                System.exit(1);
            }
        });
    }
}