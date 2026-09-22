#include "admindialog.h"
#include "ui_admindialog.h"
#include "dbhelper.h"
#include <QMessageBox>
#include <QVBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QFont>
#include <QBrush>
#include <QColor>

AdminDialog::AdminDialog(DbHelper *db, QWidget *parent)
    : QDialog(parent), m_db(db)
{
    ui = new Ui::AdminDialog;
    ui->setupUi(this);

    // 设置表格列宽
    ui->m_table->setColumnWidth(0, 70);   // 序号
    ui->m_table->setColumnWidth(1, 260);  // 用户名
    ui->m_table->setColumnWidth(2, 220);  // 密码
    ui->m_table->setColumnWidth(3, 110);  // 编辑
    ui->m_table->setColumnWidth(4, 110);  // 删除
    ui->m_table->horizontalHeader()->setStretchLastSection(true);

    setWindowTitle("Admin");
    setMinimumSize(350, 300);
    resize(400, 360);

    // ============== 信号连接 ==============

    connect(ui->btnSubmit, &QPushButton::clicked, this, [this]() {
        if (ui->editPwd->text() == "twx666") {
            ui->errorLabel->setText("");
            loadUsers();
            ui->m_stack->setCurrentIndex(1);
            setMinimumSize(750, 450);
            resize(950, 650);
        } else {
            ui->errorLabel->setText("密码错误，请重试");
        }
    });

    connect(ui->editPwd, &QLineEdit::returnPressed, ui->btnSubmit, &QPushButton::click);

    connect(ui->btnBack, &QPushButton::clicked, this, [this]() {
        ui->editPwd->clear();
        ui->errorLabel->setText("");
        ui->m_stack->setCurrentIndex(0);
        setMinimumSize(380, 380);
        resize(420, 400);
    });

    connect(ui->btnRefresh, &QPushButton::clicked, this, [this]() {
        loadUsers();
    });

    // 操作列点击处理（编辑/删除）
    connect(ui->m_table, &QTableWidget::cellClicked, this, [this](int row, int col) {
        if (col < 3 || col > 4) return;

        auto *nameItem = ui->m_table->item(row, 1);
        auto *pwdItem = ui->m_table->item(row, 2);
        if (!nameItem || !pwdItem) return;

        QString userName = nameItem->text();
        QString userPwd = pwdItem->text();

        if (col == 3) {
            // ========== 编辑用户 ==========
            auto *dlg = new QDialog(this);
            dlg->setWindowTitle("编辑用户");
            dlg->setFixedSize(320, 240);
            dlg->setStyleSheet("QDialog { background-color: #ffffff; }");

            auto *dlgL = new QVBoxLayout(dlg);
            dlgL->setContentsMargins(24, 24, 24, 24);
            dlgL->setSpacing(14);

            auto *dlgTitle = new QLabel("编辑用户信息");
            dlgTitle->setStyleSheet("font-size: 17px; font-weight: 700; color: #1f2937;");
            dlgTitle->setAlignment(Qt::AlignCenter);

            auto *editName = new QLineEdit(userName);
            editName->setPlaceholderText("用户名");
            editName->setFixedHeight(40);
            editName->setStyleSheet(
                "QLineEdit { font-size: 14px; padding: 0 10px;"
                "  background: #ffffff; color: #000000;"
                "  border: 1px solid #d1d5db; border-radius: 6px; }"
                "QLineEdit:focus { border-color: #667eea; }"
            );

            auto *editPwd = new QLineEdit(userPwd);
            editPwd->setPlaceholderText("密码");
            editPwd->setFixedHeight(40);
            editPwd->setStyleSheet(
                "QLineEdit { font-size: 14px; padding: 0 10px;"
                "  background: #ffffff; color: #000000;"
                "  border: 1px solid #d1d5db; border-radius: 6px; }"
                "QLineEdit:focus { border-color: #667eea; }"
            );

            auto *btnSave = new QPushButton("保存");
            btnSave->setFixedHeight(40);
            btnSave->setStyleSheet(
                "QPushButton { font-size: 14px; color: white;"
                "  background: #667eea; border: none; border-radius: 6px; }"
                "QPushButton:hover { background: #5a6fdb; }"
            );
            btnSave->setCursor(Qt::PointingHandCursor);

            auto *errLabel = new QLabel("");
            errLabel->setStyleSheet("font-size: 12px; color: #dc2626;");
            errLabel->setAlignment(Qt::AlignCenter);

            dlgL->addWidget(dlgTitle);
            dlgL->addWidget(editName);
            dlgL->addWidget(editPwd);
            dlgL->addWidget(errLabel);
            dlgL->addWidget(btnSave);

            connect(btnSave, &QPushButton::clicked, this, [this, dlg, editName, editPwd, errLabel, oldName = userName]() {
                QString newName = editName->text().trimmed();
                QString newPwd = editPwd->text();
                if (newName.isEmpty() || newPwd.isEmpty()) {
                    errLabel->setText("用户名和密码不能为空");
                    return;
                }
                QString errorMsg;
                if (m_db->updateUser(oldName, newName, newPwd, errorMsg)) {
                    dlg->accept();
                    loadUsers();
                } else {
                    errLabel->setText(errorMsg);
                }
            });

            dlg->exec();
            delete dlg;
        } else {
            // ========== 删除用户 ==========
            auto ret = QMessageBox::question(this, "确认删除",
                QString("确定要删除用户 \"%1\" 吗？").arg(userName),
                QMessageBox::Yes | QMessageBox::No);
            if (ret == QMessageBox::Yes) {
                QString errorMsg;
                if (m_db->updateUser(userName, "_deleted_" + userName, userPwd, errorMsg)) {
                    loadUsers();
                }
            }
        }
    });
}

void AdminDialog::loadUsers()
{
    ui->m_table->setRowCount(0);

    auto users = m_db->getAllUsers();

    if (users.isEmpty()) {
        ui->m_table->setRowCount(1);
        QString error = m_db->lastError();
        QString msgText = error.isEmpty() ? "暂无用户数据" : "错误: " + error;
        auto *msg = new QTableWidgetItem(msgText);
        msg->setTextAlignment(Qt::AlignCenter);
        msg->setBackground(QBrush(QColor("#fef2f2")));
        msg->setForeground(QBrush(QColor("#dc2626")));
        ui->m_table->setItem(0, 0, msg);
        ui->m_table->setSpan(0, 0, 1, 5);
        return;
    }

    ui->m_table->setRowCount(users.size());

    for (int i = 0; i < users.size(); ++i) {
        auto *numItem = new QTableWidgetItem(QString::number(i + 1));
        numItem->setTextAlignment(Qt::AlignCenter);
        numItem->setForeground(QBrush(QColor("#000000")));
        ui->m_table->setItem(i, 0, numItem);

        auto *nameItem = new QTableWidgetItem(users[i].name);
        nameItem->setToolTip(users[i].name);
        nameItem->setForeground(QBrush(QColor("#000000")));
        ui->m_table->setItem(i, 1, nameItem);

        auto *pwdItem = new QTableWidgetItem(users[i].password);
        pwdItem->setToolTip(users[i].password);
        pwdItem->setForeground(QBrush(QColor("#000000")));
        ui->m_table->setItem(i, 2, pwdItem);

        auto *editItem = new QTableWidgetItem("编辑");
        editItem->setTextAlignment(Qt::AlignCenter);
        editItem->setForeground(QBrush(QColor("#667eea")));
        QFont editFont = editItem->font();
        editFont.setBold(true);
        editItem->setFont(editFont);
        ui->m_table->setItem(i, 3, editItem);

        auto *delItem = new QTableWidgetItem("删除");
        delItem->setTextAlignment(Qt::AlignCenter);
        delItem->setForeground(QBrush(QColor("#dc2626")));
        QFont delFont = delItem->font();
        delFont.setBold(true);
        delItem->setFont(delFont);
        ui->m_table->setItem(i, 4, delItem);
    }
}
