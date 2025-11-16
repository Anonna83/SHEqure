package com.example.mypanicapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypanicapp.R
import org.json.JSONArray
import org.json.JSONObject

// Data class for a contact
data class Contact(val name: String, val number: String) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("number", number)
        return obj
    }
    companion object {
        fun fromJson(obj: JSONObject): Contact {
            return Contact(obj.getString("name"), obj.getString("number"))
        }
    }
}

class SettingsFragment : Fragment() {
    private lateinit var editTextName: EditText
    private lateinit var editTextNumber: EditText
    private lateinit var buttonAdd: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private val sharedPrefKey = "favorite_contacts"
    private var contactsList = mutableListOf<Contact>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        editTextName = view.findViewById(R.id.editTextName)
        editTextNumber = view.findViewById(R.id.editTextNumber)
        buttonAdd = view.findViewById(R.id.buttonAdd)
        recyclerView = view.findViewById(R.id.recyclerViewNumbers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactsAdapter(contactsList) { contact -> removeContact(contact) }
        recyclerView.adapter = adapter

        loadContacts()

        buttonAdd.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val number = editTextNumber.text.toString().trim()
            if (name.isNotEmpty() && number.isNotEmpty() && !contactsList.any { it.number == number }) {
                contactsList.add(Contact(name, number))
                saveContacts()
                adapter.notifyDataSetChanged()
                editTextName.text.clear()
                editTextNumber.text.clear()
            } else {
                Toast.makeText(requireContext(), "Enter a unique name and number", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadContacts() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val json = prefs.getString(sharedPrefKey, "[]") ?: "[]"
        val arr = JSONArray(json)
        contactsList.clear()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            contactsList.add(Contact.fromJson(obj))
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveContacts() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val arr = JSONArray()
        contactsList.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(sharedPrefKey, arr.toString()).apply()
    }

    private fun removeContact(contact: Contact) {
        contactsList.remove(contact)
        saveContacts()
        adapter.notifyDataSetChanged()
    }

    class ContactsAdapter(
        private val contacts: List<Contact>,
        private val onLongClick: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val contact = contacts[position]
            holder.bind(contact, onLongClick)
        }

        override fun getItemCount() = contacts.size

        class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(contact: Contact, onLongClick: (Contact) -> Unit) {
                val text1 = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
                val text2 = itemView.findViewById<android.widget.TextView>(android.R.id.text2)
                text1.text = contact.name
                text2.text = contact.number
                itemView.setOnLongClickListener {
                    onLongClick(contact)
                    true
                }
            }
        }
    }
} 