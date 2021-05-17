package com.softbankrobotics.pddlplayground.util

import android.content.Context
import com.softbankrobotics.pddlplanning.Tasks
import com.softbankrobotics.pddlplayground.MainActivity.Companion.planSearchFunction
import com.softbankrobotics.pddlplayground.data.DatabaseHelper

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

    fun getSubtypes(type: String?, context: Context): List<String> {
        if (type.isNullOrEmpty())
            return emptyList()
        val types = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        return getSubtypes(type, types)
    }

    private fun getSubtypes(type: String, types: List<String?>): List<String> {
        val subtypeList = mutableListOf<String>()
        val children = types.filter {
            it?.substringAfter(" - ") == type
        }.map {
            it?.substringBefore(" - ")
        }
        return if (children.isNullOrEmpty())
            listOf(type)
        else {
            for (child in children) {
                if (!child.isNullOrEmpty())
                    subtypeList.addAll(getSubtypes(child, types))
            }
            subtypeList.plus(type)
        }
    }

    fun isObjectOfType(objectLabel: String?, type: String?, context: Context): Boolean {
        if (objectLabel.isNullOrEmpty() || type.isNullOrEmpty())
            return false
        return if (objectLabel.substringAfter(" - ") == type) // simple case
            true
        else { // check parent type
            val types = DatabaseHelper.getInstance(context).getExpressions()
                .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
                .map { it.getLabel() }
            val objectType = objectLabel.substringAfter(" - ")
            isOfType(objectType, type, types)
        }
    }

    private fun isOfType(child: String, type: String, types: List<String?>): Boolean {
        val parent = types.first {
            it?.substringBefore( " - ") == child
        }?.substringAfter(" - ")
        return if (parent == type)
            true
        else if (parent == "object")
            false
        else if (!parent.isNullOrEmpty())
            isOfType(parent, type, types)
        else false
    }
}