package com.jeepchief.photomemorial.view

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding
import com.jeepchief.photomemorial.databinding.LayoutProgressDialogBinding

object DialogHelper {
    /**
     * 확인, 취소가 있는 기본 Dialog
     * negative가 NULL이면 확인 버튼만 출력
     * @param context
     * @param title
     * @param msg
     * @param positive
     * @param negative null 입력하면 취소 버튼 없음, 입력 안하면 dismiss 기능만 있는 취소 버튼
     */
    fun basicDialog(
        context: Context,
        title: String,
        msg: String,
        positive: DialogInterface.OnClickListener,
        negative: DialogInterface.OnClickListener? = DialogInterface.OnClickListener { _, _ -> }
    ) : AlertDialog.Builder {
        val dlg = AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("확인", positive)

        negative?.let {
            dlg.setNegativeButton("취소", it)
        }

        return dlg
    }

    /**
     * Custom Dialog
     * @param context
     * @param backgroundRes 백그라운드에 적용할 Drawable resId
     * @param view inflate 되어있는 ViewBinding 객체
     */
    fun customDialog(
        context: Context,
        backgroundRes: Int? = null,
        view: (AlertDialog) -> ViewBinding
    ) : AlertDialog {
        return AlertDialog.Builder(context).create().apply {
            setView(view.invoke(this).root)
            setCancelable(false)
            backgroundRes?.let { window?.setBackgroundDrawableResource(it) }
        }
    }

    private var progressDlg: AlertDialog? = null
    @Synchronized
    fun progressDialog(
        context: Context
    ) : AlertDialog {
        return progressDlg ?: run {
            progressDlg = AlertDialog.Builder(context).create().apply {
                val view = LayoutProgressDialogBinding.inflate(LayoutInflater.from(context))
                setView(view.root)
                setCancelable(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }

            progressDlg!!
        }
    }
}