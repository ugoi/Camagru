import { checkUserAuthentication } from "../services/auth-service.js";
import { getSettings, patchSettings } from "./model.js";

document
  .getElementById("saveSettingsButton")
  .addEventListener("click", saveSettings);

document.addEventListener("DOMContentLoaded", () => {
  try {
    const isLoggedIn = checkUserAuthentication();
    if (!isLoggedIn) {
      window.location.href = "/login";
    } else {
      const camagruHeader = document.getElementsByTagName("camagru-header")[0];
      camagruHeader.setAttribute("is-logged-in", checkUserAuthentication());

      loadSettings();
    }
  } catch (error) {
    showErrorMessage("Failed to load user data. Please try again.");
  }
});

function showSuccessMessage() {
  const successMessage = document.getElementById("successMessage");
  successMessage.classList.add("show");

  // Hide the message after 3 seconds
  setTimeout(() => {
    successMessage.classList.remove("show");
  }, 3000);
}

function showErrorMessage(message) {
  const errorMessage = document.getElementById("errorMessage");
  errorMessage.querySelector("span").textContent = message;
  errorMessage.classList.add("show");

  // Hide the message after 3 seconds
  setTimeout(() => {
    errorMessage.classList.remove("show");
  }, 3000);
}

export async function saveSettings() {
  const emailNotifications =
    document.getElementById("emailNotifications").checked;

  try {
    await patchSettings(emailNotifications);
    showSuccessMessage();
  } catch (error) {
    showErrorMessage(error.message);
  }
}

export async function loadSettings() {
  try {
    const settings = await getSettings();
    document.getElementById("emailNotifications").checked =
      settings.enable_email_notifications;
  } catch (error) {
    showErrorMessage(error.message);
  }
}
