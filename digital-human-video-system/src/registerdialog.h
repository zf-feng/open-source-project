#ifndef REGISTERDIALOG_H
#define REGISTERDIALOG_H

#include <QDialog>

class DbHelper;

namespace Ui {
class RegisterDialog;
}

class RegisterDialog : public QDialog
{
    Q_OBJECT
public:
    explicit RegisterDialog(DbHelper *db, QWidget *parent = nullptr);
    ~RegisterDialog();

private:
    DbHelper *m_db;
    Ui::RegisterDialog *ui;
};

#endif // REGISTERDIALOG_H
