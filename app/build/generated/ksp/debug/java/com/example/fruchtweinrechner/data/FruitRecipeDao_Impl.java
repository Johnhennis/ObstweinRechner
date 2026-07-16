package com.example.fruchtweinrechner.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FruitRecipeDao_Impl implements FruitRecipeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FruitRecipe> __insertionAdapterOfFruitRecipe;

  private final EntityInsertionAdapter<FruitRecipe> __insertionAdapterOfFruitRecipe_1;

  private final EntityDeletionOrUpdateAdapter<FruitRecipe> __deletionAdapterOfFruitRecipe;

  private final EntityDeletionOrUpdateAdapter<FruitRecipe> __updateAdapterOfFruitRecipe;

  public FruitRecipeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFruitRecipe = new EntityInsertionAdapter<FruitRecipe>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `fruit_recipes` (`id`,`name`,`saftAusbeute`,`saftAnteilImWein`,`zuckerProLiter`,`hefeProLiter`,`naehrsalzProLiter`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FruitRecipe entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getSaftAusbeute());
        statement.bindDouble(4, entity.getSaftAnteilImWein());
        statement.bindDouble(5, entity.getZuckerProLiter());
        statement.bindDouble(6, entity.getHefeProLiter());
        statement.bindDouble(7, entity.getNaehrsalzProLiter());
      }
    };
    this.__insertionAdapterOfFruitRecipe_1 = new EntityInsertionAdapter<FruitRecipe>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `fruit_recipes` (`id`,`name`,`saftAusbeute`,`saftAnteilImWein`,`zuckerProLiter`,`hefeProLiter`,`naehrsalzProLiter`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FruitRecipe entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getSaftAusbeute());
        statement.bindDouble(4, entity.getSaftAnteilImWein());
        statement.bindDouble(5, entity.getZuckerProLiter());
        statement.bindDouble(6, entity.getHefeProLiter());
        statement.bindDouble(7, entity.getNaehrsalzProLiter());
      }
    };
    this.__deletionAdapterOfFruitRecipe = new EntityDeletionOrUpdateAdapter<FruitRecipe>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `fruit_recipes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FruitRecipe entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfFruitRecipe = new EntityDeletionOrUpdateAdapter<FruitRecipe>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `fruit_recipes` SET `id` = ?,`name` = ?,`saftAusbeute` = ?,`saftAnteilImWein` = ?,`zuckerProLiter` = ?,`hefeProLiter` = ?,`naehrsalzProLiter` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FruitRecipe entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getSaftAusbeute());
        statement.bindDouble(4, entity.getSaftAnteilImWein());
        statement.bindDouble(5, entity.getZuckerProLiter());
        statement.bindDouble(6, entity.getHefeProLiter());
        statement.bindDouble(7, entity.getNaehrsalzProLiter());
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final FruitRecipe recipe, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfFruitRecipe.insertAndReturnId(recipe);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<FruitRecipe> recipes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFruitRecipe_1.insert(recipes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final FruitRecipe recipe, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFruitRecipe.handle(recipe);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final FruitRecipe recipe, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFruitRecipe.handle(recipe);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FruitRecipe>> getAll() {
    final String _sql = "SELECT * FROM fruit_recipes ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"fruit_recipes"}, new Callable<List<FruitRecipe>>() {
      @Override
      @NonNull
      public List<FruitRecipe> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSaftAusbeute = CursorUtil.getColumnIndexOrThrow(_cursor, "saftAusbeute");
          final int _cursorIndexOfSaftAnteilImWein = CursorUtil.getColumnIndexOrThrow(_cursor, "saftAnteilImWein");
          final int _cursorIndexOfZuckerProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "zuckerProLiter");
          final int _cursorIndexOfHefeProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "hefeProLiter");
          final int _cursorIndexOfNaehrsalzProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "naehrsalzProLiter");
          final List<FruitRecipe> _result = new ArrayList<FruitRecipe>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FruitRecipe _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpSaftAusbeute;
            _tmpSaftAusbeute = _cursor.getDouble(_cursorIndexOfSaftAusbeute);
            final double _tmpSaftAnteilImWein;
            _tmpSaftAnteilImWein = _cursor.getDouble(_cursorIndexOfSaftAnteilImWein);
            final double _tmpZuckerProLiter;
            _tmpZuckerProLiter = _cursor.getDouble(_cursorIndexOfZuckerProLiter);
            final double _tmpHefeProLiter;
            _tmpHefeProLiter = _cursor.getDouble(_cursorIndexOfHefeProLiter);
            final double _tmpNaehrsalzProLiter;
            _tmpNaehrsalzProLiter = _cursor.getDouble(_cursorIndexOfNaehrsalzProLiter);
            _item = new FruitRecipe(_tmpId,_tmpName,_tmpSaftAusbeute,_tmpSaftAnteilImWein,_tmpZuckerProLiter,_tmpHefeProLiter,_tmpNaehrsalzProLiter);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super FruitRecipe> $completion) {
    final String _sql = "SELECT * FROM fruit_recipes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FruitRecipe>() {
      @Override
      @Nullable
      public FruitRecipe call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSaftAusbeute = CursorUtil.getColumnIndexOrThrow(_cursor, "saftAusbeute");
          final int _cursorIndexOfSaftAnteilImWein = CursorUtil.getColumnIndexOrThrow(_cursor, "saftAnteilImWein");
          final int _cursorIndexOfZuckerProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "zuckerProLiter");
          final int _cursorIndexOfHefeProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "hefeProLiter");
          final int _cursorIndexOfNaehrsalzProLiter = CursorUtil.getColumnIndexOrThrow(_cursor, "naehrsalzProLiter");
          final FruitRecipe _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpSaftAusbeute;
            _tmpSaftAusbeute = _cursor.getDouble(_cursorIndexOfSaftAusbeute);
            final double _tmpSaftAnteilImWein;
            _tmpSaftAnteilImWein = _cursor.getDouble(_cursorIndexOfSaftAnteilImWein);
            final double _tmpZuckerProLiter;
            _tmpZuckerProLiter = _cursor.getDouble(_cursorIndexOfZuckerProLiter);
            final double _tmpHefeProLiter;
            _tmpHefeProLiter = _cursor.getDouble(_cursorIndexOfHefeProLiter);
            final double _tmpNaehrsalzProLiter;
            _tmpNaehrsalzProLiter = _cursor.getDouble(_cursorIndexOfNaehrsalzProLiter);
            _result = new FruitRecipe(_tmpId,_tmpName,_tmpSaftAusbeute,_tmpSaftAnteilImWein,_tmpZuckerProLiter,_tmpHefeProLiter,_tmpNaehrsalzProLiter);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM fruit_recipes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
