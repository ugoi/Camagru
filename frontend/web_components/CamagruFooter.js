import { logout } from "../login.js";

const template = document.createElement("template");
template.innerHTML = `    <footer
      style="

        background-color: #333;
        color: white;
        text-align: center;
        padding: 10px 0;
        position: fixed;
        bottom: 0;
        width: 100%;
      "
    >
      <p>&copy; 2024 CamGuru</p>
    </footer>`;

/**
 * @slot container - You can put some elements here
 *
 * @cssprop --text-color - Controls the color of foo
 * @cssproperty --background-color - Controls the color of bar
 *
 * @csspart bar - Styles the color of bar
 */
class CamagruFooter extends HTMLElement {
  constructor() {
    super();
    const shadow = this.attachShadow({ mode: "open" });
    shadow.append(template.content.cloneNode(true));
  }

  handleLogout() {
    logout();
  }
}

customElements.define("camagru-footer", CamagruFooter);
