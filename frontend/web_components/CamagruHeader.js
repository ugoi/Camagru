import { logout } from "../login/login.js";

const template = document.createElement("template");
template.innerHTML = `    
    <link
      rel="stylesheet"
      href="/node_modules/@fortawesome/fontawesome-free/css/all.min.css"
    />
    <header style="  background: linear-gradient(to right, slateblue, lightblue); color: white; padding: 15px 0; grid-area: header">
      <nav
        style="
          max-width: 1000px;
          margin: 0 auto;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 20px;
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
            <a href="/index.html" style="color: white; text-decoration: none; font-size: 1.2em;" title="Home">
              <svg style="width: 20px; height: 20px;" viewBox="0 0 576 512">
                <path fill="currentColor" d="M575.8 255.5c0 18-15 32.1-32 32.1h-32l.7 160.2c0 2.7-.2 5.4-.5 8.1V472c0 22.1-17.9 40-40 40H456c-1.1 0-2.2 0-3.3-.1c-1.4 .1-2.8 .1-4.2 .1H416 392c-22.1 0-40-17.9-40-40V448 384c0-17.7-14.3-32-32-32H256c-17.7 0-32 14.3-32 32v64 24c0 22.1-17.9 40-40 40H160 128.1c-1.5 0-3-.1-4.5-.2c-1.2 .1-2.4 .2-3.6 .2H104c-22.1 0-40-17.9-40-40V360c0-.9 0-1.9 .1-2.8V287.6H32c-18 0-32-14-32-32.1c0-9 3-17 10-24L266.4 8c7-7 15-8 22-8s15 2 21 7L564.8 231.5c8 7 12 15 11 24z"/>
              </svg>
            </a>
          </li>
          <li>
            <a href="/feed" style="color: white; text-decoration: none; font-size: 1.2em;" title="Feed">
              <svg style="width: 20px; height: 20px;" viewBox="0 0 576 512">
                <path fill="currentColor" d="M88.7 223.8L0 375.8V96C0 60.7 28.7 32 64 32H181.5c17 0 33.3 6.7 45.3 18.7l26.5 26.5c12 12 28.3 18.7 45.3 18.7H416c35.3 0 64 28.7 64 64v32H144c-22.8 0-43.8 12.1-55.3 31.8zm27.6 16.1C122.1 230 132.6 224 144 224H544c11.5 0 22 6.1 27.7 16.1s5.7 22.2-.1 32.1l-112 192C453.9 474 443.4 480 432 480H32c-11.5 0-22-6.1-27.7-16.1s-5.7-22.2 .1-32.1l112-192z"/>
              </svg>
            </a>
          </li>
          <li>
            <a href="/webcam" style="color: white; text-decoration: none; font-size: 1.2em;" title="Create">
              <svg style="width: 20px; height: 20px;" viewBox="0 0 512 512">
                <path fill="currentColor" d="M149.1 64.8L138.7 96H64C28.7 96 0 124.7 0 160V416c0 35.3 28.7 64 64 64H448c35.3 0 64-28.7 64-64V160c0-35.3-28.7-64-64-64H373.3L362.9 64.8C356.4 45.2 338.1 32 317.4 32H194.6c-20.7 0-39 13.2-45.5 32.8zM256 192a96 96 0 1 1 0 192 96 96 0 1 1 0-192z"/>
              </svg>
            </a>
          </li>
          <li>
            <a href="/profile" style="color: white; text-decoration: none; font-size: 1.2em;" title="Profile">
              <svg style="width: 20px; height: 20px;" viewBox="0 0 448 512">
                <path fill="currentColor" d="M224 256A128 128 0 1 0 224 0a128 128 0 1 0 0 256zm-45.7 48C79.8 304 0 383.8 0 482.3C0 498.7 13.3 512 29.7 512H418.3c16.4 0 29.7-13.3 29.7-29.7C448 383.8 368.2 304 269.7 304H178.3z"/>
              </svg>
            </a>
          </li>
          <li>
            <a href="/settings" style="color: white; text-decoration: none; font-size: 1.2em;" title="Settings">
              <svg style="width: 20px; height: 20px;" viewBox="0 0 512 512">
                <path fill="currentColor" d="M495.9 166.6c3.2 8.7 .5 18.4-6.4 24.6l-43.3 39.4c1.1 8.3 1.7 16.8 1.7 25.4s-.6 17.1-1.7 25.4l43.3 39.4c6.9 6.2 9.6 15.9 6.4 24.6c-4.4 11.9-9.7 23.3-15.8 34.3l-4.7 8.1c-6.6 11-14 21.4-22.1 31.2c-5.9 7.2-15.7 9.6-24.5 6.8l-55.7-17.7c-13.4 10.3-28.2 18.9-44 25.4l-12.5 57.1c-2 9.1-9 16.3-18.2 17.8c-13.8 2.3-28 3.5-42.5 3.5s-28.7-1.2-42.5-3.5c-9.2-1.5-16.2-8.7-18.2-17.8l-12.5-57.1c-15.8-6.5-30.6-15.1-44-25.4L83.1 425.9c-8.8 2.8-18.6 .3-24.5-6.8c-8.1-9.8-15.5-20.2-22.1-31.2l-4.7-8.1c-6.1-11-11.4-22.4-15.8-34.3c-3.2-8.7-.5-18.4 6.4-24.6l43.3-39.4C64.6 273.1 64 264.6 64 256s.6-17.1 1.7-25.4L22.4 191.2c-6.9-6.2-9.6-15.9-6.4-24.6c4.4-11.9 9.7-23.3 15.8-34.3l4.7-8.1c6.6-11 14-21.4 22.1-31.2c5.9-7.2 15.7-9.6 24.5-6.8l55.7 17.7c13.4-10.3 28.2-18.9 44-25.4l12.5-57.1c2-9.1 9-16.3 18.2-17.8C227.3 1.2 241.5 0 256 0s28.7 1.2 42.5 3.5c9.2 1.5 16.2 8.7 18.2 17.8l12.5 57.1c15.8 6.5 30.6 15.1 44 25.4l55.7-17.7c8.8-2.8 18.6-.3 24.5 6.8c8.1 9.8 15.5 20.2 22.1 31.2l4.7 8.1c6.1 11 11.4 22.4 15.8 34.3zM256 336a80 80 0 1 0 0-160 80 80 0 1 0 0 160z"/>
              </svg>
            </a>
          </li>
        </ul>
        <div>
          <button
            id="login-btn"
            onclick="window.location.href='/login'"
            style="
              background-color: transparent;
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
              background-color: transparent;
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
