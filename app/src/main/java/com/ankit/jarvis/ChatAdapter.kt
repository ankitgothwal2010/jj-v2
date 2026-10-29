/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ankit.jarvis.databinding.ItemMessageBinding

/**
 * RecyclerView Adapter for displaying local conversation history with HUD-styled message cards.
 */
class ChatAdapter(private val items: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMessageBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = items[position]
        val context = holder.itemView.context

        val params = holder.binding.messageCard.layoutParams as LinearLayout.LayoutParams
        when (msg.sender) {
            ChatMessage.Sender.USER -> {
                params.gravity = Gravity.END
                holder.binding.tvSenderLabel.text = "YOU • ${msg.formattedTime()}"
                holder.binding.tvSenderLabel.setTextColor(ContextCompat.getColor(context, R.color.jarvis_cyan))
                holder.binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.jarvis_user_bubble))
                holder.binding.messageCard.strokeColor = ContextCompat.getColor(context, R.color.jarvis_cyan)
            }
            ChatMessage.Sender.JARVIS -> {
                params.gravity = Gravity.START
                holder.binding.tvSenderLabel.text = "JARVIS [${msg.sourceBadge}] • ${msg.formattedTime()}"
                holder.binding.tvSenderLabel.setTextColor(ContextCompat.getColor(context, R.color.jarvis_blue_light))
                holder.binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.jarvis_ai_bubble))
                holder.binding.messageCard.strokeColor = ContextCompat.getColor(context, R.color.jarvis_blue)
            }
            ChatMessage.Sender.SYSTEM -> {
                params.gravity = Gravity.CENTER_HORIZONTAL
                holder.binding.tvSenderLabel.text = "SYSTEM TELEMETRY • ${msg.formattedTime()}"
                holder.binding.tvSenderLabel.setTextColor(ContextCompat.getColor(context, R.color.jarvis_text_secondary))
                holder.binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.jarvis_surface))
                holder.binding.messageCard.strokeColor = ContextCompat.getColor(context, R.color.jarvis_border_dim)
            }
        }
        holder.binding.messageCard.layoutParams = params
        holder.binding.tvMessageBody.text = msg.text
    }

    override fun getItemCount(): Int = items.size

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun replaceAll(newItems: List<ChatMessage>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
