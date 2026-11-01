# ChillMates 🏠

**ChillMates** is an Android app for people who share a flat. It puts the everyday parts of living together in one place: chores rotation, the shopping list, shared expenses, house news and repair requests. The goal is fewer forgotten tasks, fewer awkward money conversations and a calmer home.

> 📚 **Coursework project.** ChillMates was built as coursework for a mobile application development course at Universidad Carlos III de Madrid (UC3M).

## How it works

One flatmate creates a flat and shares an invite code. Everyone who joins sees the same data, synced in real time.

- 🏡 **Home**: a notice board with RSVP polls, plus upcoming events with calendar reminders.
- ✅ **Chores**: assign chores that rotate automatically every week, swap them and send reminders.
- 🛒 **Shopping list**: a shared list you can filter by supermarket, with supermarkets shown on a map.
- 💶 **Expenses**: split costs, attach receipts and see automatically calculated balances.
- 🔧 **Repairs**: report problems with photos. Gemini AI drafts the email to the landlord.
- ⚙️ **Settings**: dark mode, notifications, and English, Spanish or Catalan.

## Tech

Kotlin · Jetpack Compose · Firebase (Auth, Firestore, Storage) · Google Maps & Places · Gemini API

To run it, add your own `google-services.json` to `app/`, and put `MAPS_API_KEY` and `GEMINI_API_KEY` in `local.properties`.
