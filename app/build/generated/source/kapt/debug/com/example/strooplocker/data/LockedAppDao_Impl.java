package com.example.strooplocker.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class LockedAppDao_Impl implements LockedAppDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LockedApp> __insertionAdapterOfLockedApp;

  private final EntityDeletionOrUpdateAdapter<LockedApp> __deletionAdapterOfLockedApp;

  public LockedAppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLockedApp = new EntityInsertionAdapter<LockedApp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `locked_apps` (`packageName`) VALUES (?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @Nullable final LockedApp entity) {
        if (entity.getPackageName() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getPackageName());
        }
      }
    };
    this.__deletionAdapterOfLockedApp = new EntityDeletionOrUpdateAdapter<LockedApp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `locked_apps` WHERE `packageName` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @Nullable final LockedApp entity) {
        if (entity.getPackageName() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getPackageName());
        }
      }
    };
  }

  @Override
  public void insertLockedApp(final LockedApp lockedApp) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLockedApp.insert(lockedApp);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteLockedApp(final LockedApp lockedApp) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfLockedApp.handle(lockedApp);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<LockedApp> getAllLockedApps() {
    final String _sql = "SELECT * FROM locked_apps";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
      final List<LockedApp> _result = new ArrayList<LockedApp>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final LockedApp _item;
        final String _tmpPackageName;
        if (_cursor.isNull(_cursorIndexOfPackageName)) {
          _tmpPackageName = null;
        } else {
          _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
        }
        _item = new LockedApp(_tmpPackageName);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
