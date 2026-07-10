The application has been successfully updated.

**Admin Panel Access**
- The "ADMIN" button logo has been successfully removed from the app interface.
- Normal users will no longer see this button.
- I have updated the backend to securely inject an `is_admin` flag on the login and profile endpoints.
- When an administrator (e.g. using `6202778501`) logs in through the normal login page, the app receives the admin state securely from the database/server.
- The app will now seamlessly route the administrator directly to the Admin Panel immediately after successful authentication.

**Action Required**
You must deploy the updated backend code for the admin authentication to take effect. Please push the changes inside the `/server` folder to Render!
