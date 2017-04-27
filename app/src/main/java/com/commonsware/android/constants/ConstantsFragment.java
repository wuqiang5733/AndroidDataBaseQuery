/***
 Copyright (c) 2008-2014 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain	a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _The Busy Coder's Guide to Android Development_
 https://commonsware.com/Android
 */

package com.commonsware.android.constants;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;

public class ConstantsFragment extends ListFragment implements
        DialogInterface.OnClickListener {
    private DatabaseHelper db = null;
    private Cursor current = null;
    private AsyncTask task = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(getActivity(), R.layout.row,
                        current, new String[]{
                        DatabaseHelper.TITLE,
                        DatabaseHelper.VALUE},
                        new int[]{R.id.title, R.id.value},
                        0);

        setListAdapter(adapter);

        if (current == null) {
            // 创建数据库
            db = new DatabaseHelper(getActivity());
            // 执行查询，并且把结果放到 ListView 当中
            task = new LoadCursorTask().execute();
        }
    }


    @Override
    public void onDestroy() {
        /**
         * Also note that in onDestroy(), as shown previously, we call cancel() on the AsyncTask if it is not null.
         * If the task is still running, calling cancel() will prevent onPostExecute() from being invoked,
         * and we will not have to worry about updating our UI after the fragment has been destroyed.
         */
        if (task != null) {
            task.cancel(false);
        }
         // 通过 getListAdapter() 获得 CursorAdapter ， 然后再通过 getCursor() 方法 获得 Cursor 并将其关闭
        //  你不能关闭一个正在被CursorAdapter使用的 Cursor
        ((CursorAdapter) getListAdapter()).getCursor().close();
        // 关闭 Cursor 之后 才能 关闭数据库
        db.close();

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {
            add();
            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    private void add() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View addView = inflater.inflate(R.layout.add_edit, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.add_title).setView(addView)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null).show();
    }

    public void onClick(DialogInterface di, int whichButton) {
        ContentValues values = new ContentValues(2);
        AlertDialog dlg = (AlertDialog) di;
        EditText title = (EditText) dlg.findViewById(R.id.title);
        EditText value = (EditText) dlg.findViewById(R.id.value);

        values.put(DatabaseHelper.TITLE, title.getText().toString());
        values.put(DatabaseHelper.VALUE, value.getText().toString());

        task = new InsertTask().execute(values);
    }

    abstract private class BaseTask<T> extends AsyncTask<T, Void, Cursor> {
        /**
         * onPostExecute() then uses changeCursor() to replace the Cursor in the SimpleCursorAdapter with the results.
         * Since our SimpleCursorAdapter was created with a null Cursor,
         * changeCursor() just slides in the new Cursor, telling the ListView that the data changed.
         * This causes our ListView to be populated.
           This way, the UI will not be frozen while the query is being executed,
           yet we only update the UI from the main application thread.
         */
        @Override
        public void onPostExecute(Cursor result) {
            ((CursorAdapter) getListAdapter()).changeCursor(result);
            current = result;
            task = null;
        }
// 只有执行 getReadableDatabase() 或者 getWritableDatabase() 的时候，才会创建数据库，所以创建数据库也是在后台执行的
        Cursor doQuery() {
            Cursor result = db.getReadableDatabase()
                    .query(DatabaseHelper.TABLE,
                            new String[]{"ROWID AS _id",
                                    DatabaseHelper.TITLE,
                                    DatabaseHelper.VALUE},
                            DatabaseHelper.TITLE + " like ?" ,  new String[] { "Gravity, M" + "%"}, null, null, DatabaseHelper.TITLE);
//                            DatabaseHelper.TITLE + "=?" ,  new String[] { "Gravity, Mars"}, null, null, DatabaseHelper.TITLE);
            // 只有需要结果集的时候，query 才会被执行，所以这里 result.getCount(); 就是这一目的
            result.getCount();

            return (result);
        }
    }

    private class LoadCursorTask extends BaseTask<Void> {
        @Override
        protected Cursor doInBackground(Void... params) {
            return (doQuery());
        }
    }

    private class InsertTask extends BaseTask<ContentValues> {
        @Override
        protected Cursor doInBackground(ContentValues... values) {
            db.getWritableDatabase().insert(DatabaseHelper.TABLE,
                    DatabaseHelper.TITLE, values[0]);
        // 插入数据之后 又执行了一次查询，这样 View 才能更新
            return (doQuery());
        }
    }
}
// 这个数据库教程不错：http://www.androidhive.info/2011/11/android-sqlite-database-tutorial/
// Query 语法最最详细的教程： http://stackoverflow.com/questions/1243199/how-to-perform-an-sqlite-query-within-an-android-application
