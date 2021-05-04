package com.softbankrobotics.pddlplayground.util

import android.content.Context
import com.softbankrobotics.pddlplanning.Tasks
import com.softbankrobotics.pddlplayground.MainActivity.Companion.planSearchFunction
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import java.lang.RuntimeException

object PDDLUtil {

    suspend fun getPlanFromDatabase(context: Context): Tasks {
        val domain = getDomainPDDLFromDatabase(context)
        val problem = getProblemPDDLFromDatabase(context)
        return planSearchFunction(domain, problem, null)
    }

    fun getDomainPDDLFromDatabase(context: Context): String {
        val expressions = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.isEnabled() } // only retrieved enabled expressions
        val types = expressions
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        val constants = expressions
            .filter { it.getCategory() == PDDLCategory.CONSTANT.ordinal }
            .map { it.getLabel() }
        val predicates = expressions
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        val actions = expressions
            .filter { it.getCategory() == PDDLCategory.ACTION.ordinal }
            .map { it.getLabel() }
        return toDomain(
            types, constants, predicates, actions
        )
    }

    fun getProblemPDDLFromDatabase(context: Context): String {
        val expressions = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.isEnabled() } // only retrieved enabled expressions
        val objects = expressions
            .filter { it.getCategory() == PDDLCategory.OBJECT.ordinal }
            .map { it.getLabel() }
        val inits = expressions
            .filter { it.getCategory() == PDDLCategory.INIT.ordinal }
            .map { it.getLabel() }
        val goals = expressions
            .filter { it.getCategory() == PDDLCategory.GOAL.ordinal }
            .map { it.getLabel() }
        return toProblem(
            objects, inits, goals
        )
    }

    private fun toDomain(types: List<String?>,
                         constants: List<String?>,
                         predicates: List<String?>,
                         actions: List<String?>): String {
        var domain = "(define (domain playground_domain)\n" +
                "  (:requirements :adl)\n"
        // types
        domain += "  (:types\n"
        for (type in types)
            domain += "    $type\n"
        domain += "  )\n"
        // constants
        domain += "  (:constants\n"
        for (constant in constants)
            domain += "    $constant\n"
        domain += "  )\n"
        // predicates
        domain += "  (:predicates\n"
        for (predicate in predicates)
            domain += "    ($predicate)\n"
        domain += "  )\n"
        // actions
        for (action in actions)
            domain += "  (:action $action)\n"
        // close domain
        domain += ")\n"
        return domain
    }

    private fun toProblem(objects: List<String?>,
                          inits: List<String?>,
                          goals: List<String?>): String {
        var problem = "(define (problem my_problem)\n" +
                "   (:domain playground_domain)\n" +
                "   (:requirements :adl)\n"
        // objects
        problem += "  (:objects\n"
        for (obj in objects)
            problem += "    $obj\n"
        problem += "  )\n"
        // inits
        problem += "  (:init\n"
        for (init in inits)
            problem += "    ($init)\n"
        problem += "  )\n"
        // goals
        problem += "  (:goal\n    (and\n"
        for (goal in goals)
            problem += "      ($goal)\n"
        problem += "    )\n  )\n"
        // close problem
        problem += ")\n"
        return problem
    }

    fun getTypeOfObject(objLabel: String, objects: List<String?>): String {
        var type: String? = null
        for (obj in objects) {
            if (obj != null && obj.contains(objLabel)) {
                type = obj.substringAfter(' ')
                break
            }
        }
        if (type == null)
            throw RuntimeException("Type cannot be null.")
        return type
    }
}