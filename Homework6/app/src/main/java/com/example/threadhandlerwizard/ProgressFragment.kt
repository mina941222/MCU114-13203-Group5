package com.example.threadhandlerwizard

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class ProgressFragment : Fragment(R.layout.frag_progress) {
    private val vm: WorkViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bar = view.findViewById<ProgressBar>(R.id.progressBar)
        val text = view.findViewById<TextView>(R.id.txt)

        vm.status.observe(viewLifecycleOwner) { s ->
            when (s) {
                "Preparing..." -> {
                    bar.isIndeterminate = true
                    text.text = s
                    bar.progress = 0
                }
                "Working" -> {
                    bar.isIndeterminate = false
                }
                "Work Finished", "Canceled" -> {
                    bar.isIndeterminate = false
                    bar.progress = 100
                    text.text = s
                }
                else -> {
                    bar.isIndeterminate = false
                    bar.progress = 0
                    text.text = s
                }
            }
        }

        vm.progress.observe(viewLifecycleOwner) { p ->
            if (vm.status.value == "Working") {
                bar.max = 100
                bar.progress = p
                text.text = "Working... %d%%".format(p)
            }
        }
    }
}