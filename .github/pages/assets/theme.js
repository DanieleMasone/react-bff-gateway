(function () {
  var root = document.documentElement;
  var storageKey = "react-bff-gateway-theme";
  var buttons = document.querySelectorAll("[data-theme-toggle]");
  var mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

  function readStoredTheme() {
    try {
      return window.localStorage.getItem(storageKey);
    } catch (error) {
      return null;
    }
  }

  function storeTheme(theme) {
    try {
      window.localStorage.setItem(storageKey, theme);
    } catch (error) {
      return;
    }
  }

  function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    buttons.forEach(function (button) {
      var icon = button.querySelector(".theme-icon");
      button.setAttribute("aria-pressed", String(theme === "dark"));
      if (icon) {
        icon.textContent = theme === "dark" ? "Light" : "Dark";
      }
    });
  }

  function preferredTheme() {
    return readStoredTheme() || (mediaQuery.matches ? "dark" : "light");
  }

  applyTheme(preferredTheme());

  buttons.forEach(function (button) {
    button.addEventListener("click", function () {
      var nextTheme = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
      storeTheme(nextTheme);
      applyTheme(nextTheme);
    });
  });

  document.querySelectorAll(".mobile-menu a").forEach(function (link) {
    link.addEventListener("click", function () {
      var menu = link.closest("details");
      menu.open = false;
      if (link.hash && link.pathname === window.location.pathname) {
        var target = document.getElementById(link.hash.slice(1));
        if (target) {
          target.setAttribute("tabindex", "-1");
          target.focus({ preventScroll: true });
        }
      }
    });
  });

  mediaQuery.addEventListener("change", function () {
    if (!readStoredTheme()) {
      applyTheme(preferredTheme());
    }
  });
})();
