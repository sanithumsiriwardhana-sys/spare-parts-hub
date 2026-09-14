(function () {
    'use strict';

    // Table search
    document.querySelectorAll('[data-table-search]').forEach(function (input) {
        var tableId = input.getAttribute('data-table-search');
        var table = document.getElementById(tableId);

        if (!table) return;

        input.addEventListener('input', function () {
            var needle = input.value.trim().toLowerCase();

            table.querySelectorAll('tbody tr[data-search-row]').forEach(function (row) {
                row.style.display =
                    row.textContent.toLowerCase().includes(needle) ? '' : 'none';
            });
        });
    });

    // Confirmation for important forms
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var message =
                form.getAttribute('data-confirm') || 'Are you sure?';

            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    // Purchase-order total preview
    function updatePoTotal() {
        var quantity = document.querySelector('[data-po-quantity]');
        var price = document.querySelector('[data-po-price]');
        var output = document.querySelector('[data-po-total]');

        if (!quantity || !price || !output) return;

        var q = Number(quantity.value || 0);
        var p = Number(price.value || 0);
        var total = q * p;

        output.textContent = Number.isFinite(total)
            ? total.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })
            : '0.00';
    }

    document
        .querySelectorAll('[data-po-quantity], [data-po-price]')
        .forEach(function (element) {
            element.addEventListener('input', updatePoTotal);
        });

    updatePoTotal();

    // Character counters
    document.querySelectorAll('[data-char-count]').forEach(function (field) {
        var target =
            document.getElementById(field.getAttribute('data-char-count'));

        if (!target) return;

        function update() {
            target.textContent = field.value.length;
        }

        field.addEventListener('input', update);
        update();
    });
})();