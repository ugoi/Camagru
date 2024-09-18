import { checkUserAuthentication } from "../services/auth-service.js";
import { getSettings, patchSettings } from "./model.js";

document
  .getElementById("saveSettingsButton")
  .addEventListener("click", saveSettings);

document.addEventListener("DOMContentLoaded", () => {
  const camagruHeader = document.getElementsByTagName("camagru-header")[0];
  camagruHeader.setAttribute("is-logged-in", checkUserAuthentication());

  loadSettings();
});

export async function saveSettings() {
  const emailNotifications =
    document.getElementById("emailNotifications").checked;

  try {
    await patchSettings(emailNotifications);
    alert(
      "Settings saved! Email Notifications: " +
        (emailNotifications ? "On" : "Off")
    );
  } catch (error) {
    alert("Failed to save settings: " + error.message);
  }
}

export async function loadSettings() {
  try {
    const settings = await getSettings();
    document.getElementById("emailNotifications").checked =
      settings.enable_email_notifications;
  } catch (error) {
    alert("Failed to load settings: " + error.message);
  }
}
