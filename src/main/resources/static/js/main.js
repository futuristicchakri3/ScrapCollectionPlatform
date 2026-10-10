/**
 * SCRAP CONNECTION — CLIENT-SIDE JAVASCRIPT
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Mobile Menu Toggle
    const mobileToggle = document.getElementById('mobileMenuBtn');
    const navMenu = document.getElementById('navMenu');
    if (mobileToggle && navMenu) {
        mobileToggle.addEventListener('click', () => {
            navMenu.classList.toggle('active');
        });
    }

    // 2. Interactive Weighing & Settlement Calculator
    initWeighingCalculator();

    // 3. Interactive Checkout Coupon Preview
    initCheckoutCouponCalculator();

    // 4. Quick-fill credentials for demo testing
    window.fillCredentials = function (mobile, password) {
        const mobileInput = document.getElementById('mobileNumber');
        const passInput = document.getElementById('password');
        if (mobileInput) mobileInput.value = mobile;
        if (passInput) passInput.value = password;
    };
});

function initWeighingCalculator() {
    const weighRows = document.querySelectorAll('.weigh-row');
    const deductionInput = document.getElementById('deductionsInput');
    const grossDisplay = document.getElementById('grossTotalDisplay');
    const netDisplay = document.getElementById('netTotalDisplay');

    if (!weighRows.length) return;

    function recalculate() {
        let grossTotal = 0;

        weighRows.forEach(row => {
            const weightInput = row.querySelector('.weight-input');
            const rateInput = row.querySelector('.rate-input');
            const lineTotalSpan = row.querySelector('.line-total-display');

            const weight = parseFloat(weightInput?.value) || 0;
            const rate = parseFloat(rateInput?.value) || 0;
            const lineTotal = weight * rate;

            if (lineTotalSpan) {
                lineTotalSpan.textContent = '₹' + lineTotal.toFixed(2);
            }
            grossTotal += lineTotal;
        });

        const deductions = parseFloat(deductionInput?.value) || 0;
        const netTotal = Math.max(0, grossTotal - deductions);

        if (grossDisplay) grossDisplay.textContent = '₹' + grossTotal.toFixed(2);
        if (netDisplay) netDisplay.textContent = '₹' + netTotal.toFixed(2);
    }

    weighRows.forEach(row => {
        const weightInput = row.querySelector('.weight-input');
        const rateInput = row.querySelector('.rate-input');
        if (weightInput) weightInput.addEventListener('input', recalculate);
        if (rateInput) rateInput.addEventListener('input', recalculate);
    });

    if (deductionInput) {
        deductionInput.addEventListener('input', recalculate);
    }

    // Initial calculation
    recalculate();
}

function initCheckoutCouponCalculator() {
    const couponCheckbox = document.getElementById('applyCouponCheckbox');
    const subtotalElem = document.getElementById('checkoutSubtotal');
    const couponElem = document.getElementById('checkoutCouponDiscount');
    const payableElem = document.getElementById('checkoutPayable');

    if (!couponCheckbox || !subtotalElem || !payableElem) return;

    const subtotal = parseFloat(subtotalElem.dataset.subtotal) || 0;
    const availableCoupon = parseFloat(couponCheckbox.dataset.available) || 0;

    function updateSummary() {
        let discount = 0;
        if (couponCheckbox.checked) {
            discount = Math.min(subtotal, availableCoupon);
        }
        const payable = Math.max(0, subtotal - discount);

        if (couponElem) {
            couponElem.textContent = '-₹' + discount.toFixed(2);
        }
        payableElem.textContent = '₹' + payable.toFixed(2);
    }

    couponCheckbox.addEventListener('change', updateSummary);
    updateSummary();
}
