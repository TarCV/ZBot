package org.bestever.bebot

class BotConversation(context: Bot) : Conversation(context) {

    override fun defineTree() {
        +"check wad <name:string>"
        +"поддерживается ли <name:string>" {
            implementedBy = BotCommandMethod(context::processFile)
        }

        +"check wad <name:string>" {
            minimalRole = AccountType.NONE
            implementedBy = BotCommandMethod(context::processFile)
        }

        +"what <property:string> is on <port:integer>?" {
            minimalRole = AccountType.NONE
            implementedBy = BotCommandMethod(context::processGet)
        }

        +"who owns <port:integer>?" {
            minimalRole = AccountType.NONE
            implementedBy = BotCommandMethod(context::processOwner)
        }

        +"what are my servers?" {
            minimalRole = AccountType.NONE
            implementedBy = BotCommandMethod(context::processServers)
        }

        +"what is uptime for <port:integer>?" {
            minimalRole = AccountType.NONE
            implementedBy = BotCommandMethod(context::calculateUptime)
        }

// TODO:        +"what engines can be used?" {
//            minimalRole = AccountType.NONE
//            implementedBy = BotCommandMethod(context::processVersions)
//        }

        +"tell me about <port:integer>"{
            minimalRole = AccountType.REGISTERED
            implementedBy = BotCommandMethod(context::processServerInfo)
        }

        +"start a new server"{
            minimalRole = AccountType.REGISTERED
            implementedBy = BotCommandMethod(context::processHost)
        }

        +"shutdown <port:integer>"{
            minimalRole = AccountType.REGISTERED
            implementedBy = BotCommandMethod(context::processKill)
        }

        +"do <what:string> with <port:integer>" {
            minimalRole = AccountType.REGISTERED
            implementedBy = BotCommandMethod(context::sendCommand)
        }

        +"protect <port:integer>" {
            minimalRole = AccountType.VIP
            implementedBy = BotCommandMethod(context::protectServer)
        }

        +"announce <message:string>"{
            minimalRole = AccountType.MODERATOR
            implementedBy = BotCommandMethod(context::globalBroadcast)
        }

        +"shutdown inactive servers"{
            minimalRole = AccountType.MODERATOR
            implementedBy = BotCommandMethod(context::processKillInactive)
        }

        +"shutdown them all"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::processKillAll)
        }

        +"shutdown all with engine <engine:string>"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::processKillVersion)
        }

        +"disable hosting"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::processOff)
        }

        +"enable hosting"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::processOn)
        }

        +"reload configuration"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::reloadConfigFile)
        }

// TODO:       +"reload engines"{
//            minimalRole = AccountType.ADMIN
//            implementedBy = BotCommandMethod(context::reloadVersions)
//        }

        +"tell all servers <command:string>"{
            minimalRole = AccountType.ADMIN
            implementedBy = BotCommandMethod(context::sendCommandAll)
        }
    }
}
