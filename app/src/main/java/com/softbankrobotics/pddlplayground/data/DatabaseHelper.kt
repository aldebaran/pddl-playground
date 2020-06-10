package com.softbankrobotics.pddlplayground.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.util.ExpressionUtil
import timber.log.Timber

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, SCHEMA) {

    companion object {
        private const val DATABASE_NAME = "expressions.db"
        private const val SCHEMA = 1

        private const val TABLE_NAME = "expressions"

        const val _ID = "_id"
        const val COL_LABEL = "label"
        const val COL_CATEGORY = "category"
        const val COL_IS_ENABLED = "is_enabled"

        private var sInstance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(context: Context): DatabaseHelper {
            if (sInstance == null) {
                sInstance = DatabaseHelper(context.applicationContext)
            }
            return sInstance!!
        }
    }

    override fun onCreate(p0: SQLiteDatabase) {
        Timber.i("Creating database...")

        val CREATE_EXPRESSIONS_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LABEL + " TEXT, " +
                COL_CATEGORY + " INTEGER NOT NULL, " +
                COL_IS_ENABLED + " INTEGER NOT NULL" +
                ");"

        p0.execSQL(CREATE_EXPRESSIONS_TABLE)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        throw UnsupportedOperationException("This shouldn't happen yet!")
    }


    fun addExpression(): Long {
        return addExpression(Expression())
    }

    private fun addExpression(expression: Expression): Long {
        Timber.i("Adding an expression...")
        return writableDatabase.insert(TABLE_NAME, null, ExpressionUtil.toContentValues(expression))
    }

    fun updateExpression(expression: Expression): Int {
        Timber.i("Upgrading an expression...")
        val where = "$_ID=?"
        val whereArgs = arrayOf(expression.getId().toString())
        return writableDatabase
            .update(TABLE_NAME, ExpressionUtil.toContentValues(expression), where, whereArgs)
    }

    fun deleteExpression(expression: Expression): Int {
        return deleteExpression(expression.getId())
    }

    private fun deleteExpression(id: Long): Int {
        Timber.i("Deleting an expression...")
        val where = "$_ID=?"
        val whereArgs = arrayOf(id.toString())
        return writableDatabase.delete(TABLE_NAME, where, whereArgs)
    }

    fun getExpressions(): List<Expression> {
        val c = readableDatabase.query(TABLE_NAME, null, null, null, null, null, null)
        return c.use { ExpressionUtil.buildExpressionList(c) }
    }
}