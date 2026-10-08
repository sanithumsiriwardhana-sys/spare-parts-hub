(function () {
    'use strict';

    document.querySelectorAll('[data-table-search]').forEach(function (input) {
        var tableId = input.getAttribute('data-table-search');
        var table = document.getElementById(tableId);
        if (!table) return;

            var needle = input.value.trim().toLowerCase();

    });


            }
        });
    });

    function updatePoTotal() {
        var quantity = document.querySelector('[data-po-quantity]');
        var price = document.querySelector('[data-po-price]');
        var output = document.querySelector('[data-po-total]');

        if (!quantity || !price || !output) return;

        var q = Number(quantity.value || 0);
        var p = Number(price.value || 0);
        var total = q * p;

            ? total.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })
            : '0.00';
    }

            element.addEventListener('input', updatePoTotal);
        });
    updatePoTotal();

    document.querySelectorAll('[data-char-count]').forEach(function (field) {
        if (!target) return;

        function update() {
            target.textContent = field.value.length;
        }

        field.addEventListener('input', update);
        update();
    });
})();