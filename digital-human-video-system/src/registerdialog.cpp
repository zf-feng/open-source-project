#include "registerdialog.h"
#include "ui_registerdialog.h"
#include "dbhelper.h"
#include "mainwindow.h"
#include <QMessageBox>

RegisterDialog::RegisterDialog(DbHelper *db, QWidget *parent)
    : QDialog(parent), m_db(db)
{
    ui = new Ui::RegisterDialog;
    ui->setupUi(this);

    setWindowTitle("注册");
    setFixedSize(380, 410);

    // 信号连接
    connect(ui->btnSubmit, &QPushButton::clicked, this, [this]() {
        if (!m_db || !m_db->isConnected()) {
            QMessageBox::warning(this, "数据库错误",
                "无法连接到数据库\n\n" + (m_db ? m_db->lastError() : "数据库未初始化"));
            return;
        }
        QString user = ui->editUser->text().trimmed();
        QString pwd = ui->editPwd->text();
        QString confirm = ui->editConfirm->text();
        auto *mw = qobject_cast<MainWindow*>(parentWidget());

        if (user.isEmpty() || pwd.isEmpty() || confirm.isEmpty()) {
            if (mw) mw->showErrorToast("请填写所有字段");
            return;
        }
        if (pwd != confirm) {
            if (mw) mw->showErrorToast("请输入相同密码");
            return;
        }
        if (pwd.length() < 6) {
            if (mw) mw->showErrorToast("密码长度不能少于6位");
            return;
        }

        QString errorMsg;
        if (m_db->registerUser(user, pwd, errorMsg)) {
            if (mw) mw->showToast("注册成功");
            accept();
        } else {
            if (mw) mw->showErrorToast(errorMsg);
        }
    });
}

RegisterDialog::~RegisterDialog()
{
    delete ui;
}
