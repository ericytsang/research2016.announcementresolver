package com.github.ericytsang.research2016.announcementresolver

import com.github.ericytsang.research2016.announcementresolver.window.AgentsWindowController
import javafx.application.Application
import javafx.stage.Stage

object Main
{
    @JvmStatic
    fun main(args:Array<String>)
    {
        Application.launch(App::class.java)
    }

    class App:Application()
    {
        override fun start(primaryStage:Stage)
        {
            AgentsWindowController.new().stage.show()
        }
    }
}
