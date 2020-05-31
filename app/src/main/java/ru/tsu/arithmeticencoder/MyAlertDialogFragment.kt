package ru.tsu.arithmeticencoder

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment


class MyAlertDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments!!.getString("title")
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setPositiveButton("OK"){dialog, which ->  }
            .setMessage("Replace User and then Click OK")
            .create()
    }

    companion object {
        fun newInstance(): MyAlertDialogFragment {
            val frag = MyAlertDialogFragment()
            val args = Bundle()
            args.putString("title", "User Error")
            frag.arguments = args
            return frag
        }
    }
}