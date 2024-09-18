import { logout } from "../login/login.js";

const template = document.createElement("template");
template.innerHTML = `    <header style="background-color: #333; color: white; padding: 15px 0; grid-area: header">
      <nav
        style="
          max-width: 1000px;
          margin: 0 auto;
          display: flex;
          justify-content: space-between;
          align-items: center;
        "
      >
        <ul
          style="
            list-style: none;
            margin: 0;
            padding: 0;
            display: flex;
            gap: 15px;
          "
        >
          <li>
            <a href="/index.html" style="color: white; text-decoration: none"
              >Home</a
            >
          </li>
          <li>
            <a href="/feed" style="color: white; text-decoration: none"
              >Feed</a
            >
          </li>
          <li>
            <a href="/profile" style="color: white; text-decoration: none"
              >Profile</a
            >
          </li>
          <li>
            <a href="/settings" style="color: white; text-decoration: none"
              >Settings</a
            >
          </li>
        </ul>
        <div>
          <button
            id="login-btn"
            onclick="window.location.href='/login'"
            style="
              background-color: #4caf50;
              color: white;
              border: none;
              padding: 5px 5px;
              cursor: pointer;
            "
          >
            Login
          </button>
          <button
            id="logout-btn"
            onclick="this.getRootNode().host.handleLogout()"
            style="
              background-color: #f44336;
              color: white;
              border: none;
              padding: 5px 5px;
              cursor: pointer;
            "
          >
            Logout
          </button>
        </div>
      </nav>
    </header>`;

/**
 * @slot container - You can put some elements here
 *
 * @cssprop --text-color - Controls the color of foo
 * @cssproperty --background-color - Controls the color of bar
 *
 * @csspart bar - Styles the color of bar
 */
class CamagruHeader extends HTMLElement {
  constructor() {
    super();
    const shadow = this.attachShadow({ mode: "open" });
    shadow.append(template.content.cloneNode(true));
    this.loginButton = shadow.querySelector("#login-btn");
    this.logoutButton = shadow.querySelector("#logout-btn");
  }

  static get observedAttributes() {
    return ["is-logged-in"];
  }

  attributeChangedCallback(name, oldValue, newValue) {
    if (name === "is-logged-in") {
      this.handleIsLoggedInChange(newValue);
    }
  }

  handleIsLoggedInChange(newValue) {
    if (newValue === "true") {
      this.loginButton.style.display = "none";
      this.logoutButton.style.display = "block";
    } else {
      this.loginButton.style.display = "block";
      this.logoutButton.style.display = "none";
    }
  }

  handleLogout() {
    logout();
  }
}

customElements.define("camagru-header", CamagruHeader);
