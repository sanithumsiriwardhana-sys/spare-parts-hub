(function () {
    'use strict';

    document.querySelectorAll('[data-table-search]').forEach(function (input) {
        var tableId = input.getAttribute('data-table-search');
        var table = document.getElementById(tableId);
        if (!table) return;
        input.addEventListener('input', function () {
            var needle = input.value.trim().toLowerCase();
            table.querySelectorAll('tbody tr[data-search-row]').forEach(function (row) {
                row.style.display = row.textContent.toLowerCase().includes(needle) ? '' : 'none';
            });
        });
    });

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var message = form.getAttribute('data-confirm') || 'Are you sure?';
            if (!window.confirm(message)) event.preventDefault();
        });
    });

    function updatePoTotal() {
        var quantity = document.querySelector('[data-po-quantity]');
        var price = document.querySelector('[data-po-price]');
        var output = document.querySelector('[data-po-total]');
        if (!quantity || !price || !output) return;
        var q = Number(quantity.value || 0);
        var p = Number(price.value || 0);
        output.textContent = Number.isFinite(q * p)
            ? (q * p).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})
            : '0.00';
    }

    document.querySelectorAll('[data-po-quantity], [data-po-price]').forEach(function (el) {
        el.addEventListener('input', updatePoTotal);
    });
    updatePoTotal();

    document.querySelectorAll('[data-char-count]').forEach(function (field) {
        var target = document.getElementById(field.getAttribute('data-char-count'));
        if (!target) return;
        var update = function () { target.textContent = field.value.length; };
        field.addEventListener('input', update);
        update();
    });
})();
