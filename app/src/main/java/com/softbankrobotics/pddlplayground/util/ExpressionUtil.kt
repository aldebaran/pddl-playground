package com.softbankrobotics.pddlplayground.util

import android.content.ContentValues
import android.database.Cursor
import com.softbankrobotics.pddlplayground.data.DatabaseHelper.Companion.COL_CATEGORY
import com.softbankrobotics.pddlplayground.data.DatabaseHelper.Companion.COL_IS_ENABLED
import com.softbankrobotics.pddlplayground.data.DatabaseHelper.Companion.COL_LABEL
import com.softbankrobotics.pddlplayground.data.DatabaseHelper.Companion._ID
import com.softbankrobotics.pddlplayground.model.Expression
import java.util.ArrayList

object ExpressionUtil {
    fun toContentValues(expression: Expression): ContentValues {
        val cv = ContentValues(3)
        cv.put(COL_LABEL, expression.getLabel())
        cv.put(COL_CATEGORY, expression.getCategory())
        cv.put(COL_IS_ENABLED, expression.isEnabled())
        return cv
    }

    fun buildExpressionList(c: Cursor): ArrayList<Expression> {
        val size = c.count
        val expressions = ArrayList<Expression>(size)
        if (c.moveToFirst()) {
            do {
                val id = c.getLong(c.getColumnIndex(_ID))
                val label = c.getString(c.getColumnIndex(COL_LABEL))
                val category = c.getInt(c.getColumnIndex(COL_CATEGORY))
                val isEnabled = c.getInt(c.getColumnIndex(COL_IS_ENABLED)) == 1
                val expression = Expression(id, label, category, isEnabled)
                expressions.add(expression)
            } while (c.moveToNext())
        }
        return expressions
    }
}