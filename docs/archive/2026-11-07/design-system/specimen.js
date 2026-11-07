"use strict";

// This specimen performs local UI simulation only. It sends no requests and stores no data.
const searchInput = document.querySelector("#patient-search");
const patientRows = [...document.querySelectorAll("#patient-rows tr")];
const resultCount = document.querySelector("#result-count");
const searchEmpty = document.querySelector("#search-empty");

function normalizeSearch(value) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[đĐ]/g, "d").toLocaleLowerCase("vi-VN").trim();
}

function filterPatients() {
  const query = normalizeSearch(searchInput.value);
  let visibleCount = 0;
  patientRows.forEach((row) => {
    const matches = normalizeSearch(row.dataset.search).includes(query);
    row.hidden = !matches;
    if (matches) visibleCount += 1;
  });
  searchEmpty.hidden = visibleCount !== 0;
  resultCount.textContent = `${visibleCount} dòng minh họa${query ? " phù hợp" : ""}`;
}

searchInput.addEventListener("input", filterPatients);
document.querySelector("#clear-search").addEventListener("click", () => {
  searchInput.value = "";
  filterPatients();
  searchInput.focus();
});

const reducedMotion = document.querySelector("#reduced-motion");
reducedMotion.addEventListener("change", () => {
  document.documentElement.dataset.reducedMotion = String(reducedMotion.checked);
});

const simulationButton = document.querySelector("#simulate-button");
const simulationLabel = document.querySelector("#simulate-button-label");
const simulationOutcome = document.querySelector("#simulation-outcome");
const simulationFeedback = document.querySelector("#simulation-feedback");
const simulationMessage = document.querySelector("#simulation-message");
const confirmationIcon = document.querySelector("#confirmation-icon");
let simulationPending = false;

simulationButton.addEventListener("click", () => {
  if (simulationPending) return;
  simulationPending = true;
  const outcome = simulationOutcome.value;
  simulationButton.disabled = true;
  simulationButton.setAttribute("aria-busy", "true");
  simulationOutcome.disabled = true;
  simulationLabel.textContent = "Đang mô phỏng…";
  confirmationIcon.setAttribute("hidden", "");
  simulationFeedback.dataset.state = "pending";
  simulationMessage.textContent = "Đang chờ kết quả giả lập. Không có yêu cầu gửi đến backend.";

  // A fixed delay demonstrates pending feedback, not real network or persistence behavior.
  window.setTimeout(() => {
    const confirmed = outcome === "confirmed";
    simulationFeedback.dataset.state = confirmed ? "confirmed" : "error";
    confirmationIcon.toggleAttribute("hidden", !confirmed);
    simulationMessage.textContent = confirmed
      ? "Đã xác nhận mô phỏng. Không có dữ liệu nào được lưu."
      : "Lỗi mô phỏng: thao tác chưa được xác nhận. Có thể chạy lại; không có dữ liệu nào được lưu.";
    simulationLabel.textContent = "Chạy lại mô phỏng";
    simulationButton.setAttribute("aria-busy", "false");
    simulationButton.disabled = false;
    simulationOutcome.disabled = false;
    simulationPending = false;
  }, 600);
});
