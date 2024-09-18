import { checkUserAuthentication } from "./services/auth-service.js";

window.addEventListener(
  "DOMContentLoaded",
  (event) => {
    const camagruHeader = document.getElementsByTagName("camagru-header")[0];
    camagruHeader.setAttribute("is-logged-in", checkUserAuthentication());
  },
  false
);
