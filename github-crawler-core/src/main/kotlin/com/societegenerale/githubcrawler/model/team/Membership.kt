package com.societegenerale.githubcrawler.model.team

import java.util.function.Consumer

class Membership {

    private val membersToTeam = HashMap<String, MutableSet<String>>()

    fun add(team: Team, members: Set<TeamMember>) {
        members.forEach(Consumer {
            membersToTeam[it.login]
                    ?.add(team.name)
                    ?: membersToTeam.put(it.login, mutableSetOf(team.name))
        })
    }

    fun getTeams(memberLogin : String) : MutableSet<String> {
        return membersToTeam[memberLogin] ?: mutableSetOf(memberLogin)
    }

    fun isEmpty() : Boolean {
        return membersToTeam.isEmpty()
    }

}