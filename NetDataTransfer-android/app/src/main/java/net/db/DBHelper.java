package net.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DBHelper extends SQLiteOpenHelper {
    private static final String db_name = "chat.db";
    private static final int db_version = 1;

    DBHelper(Context context) {
        super(context, db_name, null, db_version);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //do nothing here
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 执行建表语句
        db.execSQL("CREATE TABLE `unread_table` (`msg_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`user_ip` varchar(20) NOT NULL," +
                "`user_name` varchar(20) NOT NULL," +
                "`chat_msg` varchar(200) NOT NULL,"+
                "`chat_data` varchar(50) NOT NULL)");
    }
}
