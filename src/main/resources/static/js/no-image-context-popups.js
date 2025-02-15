(function () {
  // Sometimes, a popup might be triggered by holding
  // a finger for too long when scrolling on mobile
  // This exists to prevent that
  document.querySelectorAll("img").forEach((img) => {
    img.style.userSelect = "none";
    img.style.webkitTouchCallout = "none";
    img.addEventListener("contextmenu", (evt) => {
      evt.preventDefault();
      return false;
    });
  });
})();
