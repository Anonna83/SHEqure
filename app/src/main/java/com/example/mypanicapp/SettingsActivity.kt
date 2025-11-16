package com.example.mypanicapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import android.widget.Spinner
import android.widget.ArrayAdapter

class SettingsActivity : AppCompatActivity() {
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

    private val sharedPrefKey = "favorite_contacts"
    private var contactsList = mutableListOf<Contact>()
    private lateinit var editTextName: EditText
    private lateinit var editTextNumber: EditText
    private lateinit var buttonAdd: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private val languagePrefKey = "selected_language"
    private val languages = listOf("English" to "en")
    private lateinit var languageSpinner: Spinner
    private lateinit var countryCodeSpinner: Spinner
    private val countryCodes = listOf(
        "+1" to "US/Canada", "+44" to "UK", "+91" to "India", "+880" to "Bangladesh",
        "+86" to "China", "+81" to "Japan", "+82" to "South Korea", "+61" to "Australia",
        "+49" to "Germany", "+33" to "France", "+39" to "Italy", "+34" to "Spain",
        "+7" to "Russia", "+971" to "UAE", "+966" to "Saudi Arabia", "+20" to "Egypt",
        "+27" to "South Africa", "+234" to "Nigeria", "+55" to "Brazil", "+52" to "Mexico",
        "+54" to "Argentina", "+351" to "Portugal", "+31" to "Netherlands", "+46" to "Sweden"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editTextName = findViewById(R.id.editTextName)
        editTextNumber = findViewById(R.id.editTextNumber)
        buttonAdd = findViewById(R.id.buttonAdd)
        recyclerView = findViewById(R.id.recyclerViewNumbers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(contactsList) { contact -> removeContact(contact) }
        recyclerView.adapter = adapter

        languageSpinner = findViewById(R.id.languageSpinner)
        countryCodeSpinner = findViewById(R.id.countryCodeSpinner)
        
        val languageNames = languages.map { it.first }
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapterSpinner

        // Set default language to English
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString(languagePrefKey, "en").apply()

        // Setup country code spinner - show only code when selected, full format in dropdown
        val countryCodeDisplay = countryCodes.map { "${it.first} (${it.second})" }
        val countryCodeAdapter = object : ArrayAdapter<String>(this, R.layout.spinner_item_country_code, countryCodeDisplay) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                // Show only country code when selected
                view.text = countryCodes[position].first
                return view
            }
        }
        countryCodeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_country_code)
        countryCodeSpinner.adapter = countryCodeAdapter

        loadContacts()

        buttonAdd.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val number = editTextNumber.text.toString().trim()
            val selectedCountryCode = countryCodes[countryCodeSpinner.selectedItemPosition].first
            val fullNumber = "$selectedCountryCode$number"
            
            if (name.isNotEmpty() && number.isNotEmpty() && !contactsList.any { it.number == fullNumber }) {
                contactsList.add(Contact(name, fullNumber))
                saveContacts()
                adapter.notifyDataSetChanged()
                editTextName.text.clear()
                editTextNumber.text.clear()
            } else {
                Toast.makeText(this, "Enter a unique name and number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadContacts() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
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
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
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
        private val onDelete: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ContactViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val contact = contacts[position]
            holder.bind(contact, onDelete)
        }

        override fun getItemCount() = contacts.size

        class ContactViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            fun bind(contact: Contact, onDelete: (Contact) -> Unit) {
                val nameText = itemView.findViewById<android.widget.TextView>(R.id.contactName)
                val numberText = itemView.findViewById<android.widget.TextView>(R.id.contactNumber)
                val deleteButton = itemView.findViewById<android.widget.ImageButton>(R.id.deleteContactButton)
                
                nameText.text = contact.name
                numberText.text = contact.number
                
                deleteButton.setOnClickListener {
                    onDelete(contact)
                }
            }
        }
    }
} 