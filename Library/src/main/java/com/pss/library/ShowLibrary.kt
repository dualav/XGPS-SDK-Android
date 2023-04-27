package com.pss.library

import android.content.Context
import android.widget.Toast

class ShowLibrary {

    companion object{

        fun sToast(context: Context, msg : String) = Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()

        fun lToast(context: Context, msg : String) = Toast.makeText(context,msg,Toast.LENGTH_LONG).show()
    }
}