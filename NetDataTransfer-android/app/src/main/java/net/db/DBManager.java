package net.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.log.Logger;
import net.vo.ChatMsgEntity;

import java.util.ArrayList;

public class DBManager {
    private static final String TAG = "DBManager";
    SQLiteDatabase db;

    public DBManager(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    public void addMsg(ChatMsgEntity msgEntity, String key) {
        Logger.info(TAG, "add record in db");
        ContentValues values = new ContentValues();
        values.put("user_ip", key);
        values.put("user_name", msgEntity.getName());
        values.put("chat_msg", msgEntity.getText());
        values.put("chat_data", msgEntity.getDate());
        db.insert("unread_table", null, values);
    }

    public ArrayList<ChatMsgEntity> queryMsg(String key) {
        Logger.info(TAG, "query record " + key);
        ArrayList<ChatMsgEntity> list = new ArrayList<>();
        String sql = "select * from unread_table where user_ip=?";
        Cursor cursor = db.rawQuery(sql, new String[]{key});

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex("user_name"));
            String msg = cursor.getString(cursor.getColumnIndex("chat_msg"));
            String data = cursor.getString(cursor.getColumnIndex("chat_data"));
            ChatMsgEntity entity = new ChatMsgEntity(name, data, msg, true);
            list.add(entity);
        }

        return list;
    }

    public void deleteMsg(String key) {
        Logger.info(TAG, "delete record in db");
        if (key == null) {
            Logger.info(TAG, "delete all");
            String sql = "delete from unread_table";
            db.execSQL(sql);
        } else {
            Logger.info(TAG, "delete single");
            String whereClause = "user_ip=?";
            String[] whereArgs = {key};
            db.delete("unread_table", whereClause, whereArgs);
        }
    }

    public boolean contains(String key) {
        Logger.info(TAG, "whether contains record in db");
        String sql = "select * from unread_table where user_ip=?";
        Cursor cursor = db.rawQuery(sql, new String[]{key});
        return cursor.getCount() != 0;
    }

    public int getUnreadMsgNum() {
        Logger.info(TAG, "get record num");
        Cursor cursor = db.query("unread_table", null, null, null, null, null, null);
        return cursor.getCount();
    }

    public void closeDB() {
        db.close();
    }
}
