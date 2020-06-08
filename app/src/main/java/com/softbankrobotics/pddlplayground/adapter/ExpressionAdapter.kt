package com.softbankrobotics.pddlplayground.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment.Companion.EDIT_EXPRESSION
import com.softbankrobotics.pddlplayground.util.PDDLCategory

class ExpressionAdapter : RecyclerView.Adapter<ExpressionAdapter.ExpressionViewHolder>() {
    private var expressions: List<Expression>? = null

    class ExpressionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView
        val delete: ImageButton
        val checkBox: CheckBox
        init {
            label = itemView.findViewById(R.id.textView)
            delete = itemView.findViewById(R.id.deleteExpression)
            checkBox = itemView.findViewById(R.id.enableExpression)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ExpressionViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.expression_row, parent, false)
        return ExpressionViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ExpressionViewHolder, position: Int) {
        val expression = expressions!![position]
        holder.label.text = expression.getLabel()
        holder.checkBox.isChecked = expression.isEnabled()
        holder.label.setOnClickListener { view ->
            val context = view.context
            val editExpressionIntent = Intent(EDIT_EXPRESSION)
                .putExtra(EDIT_EXPRESSION, expression)
            LocalBroadcastManager.getInstance(context).sendBroadcast(editExpressionIntent)
        }
        holder.checkBox.setOnCheckedChangeListener { view, isChecked ->
            expression.setEnabled(isChecked)
            DatabaseHelper.getInstance(view.context!!).updateExpression(expression)
        }
        holder.delete.setOnClickListener {view ->
            val context = view.context
            //TODO: delete item from dialog fragment instead?
            val rowsDeleted: Int = DatabaseHelper.getInstance(context!!).deleteExpression(expression)
            if (rowsDeleted == 1)
                LoadExpressionsService.launchLoadExpressionsService(context)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return if (expressions == null) 0 else expressions!!.size
    }

    fun setExpressions(expressions: List<Expression>, catergory: PDDLCategory) {
        this.expressions = expressions.filter { it.getCategory() == catergory.ordinal }
        notifyDataSetChanged()
    }
}
