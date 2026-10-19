/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */


document.addEventListener('DOMContentLoaded', function () {

    const linkPolitica = document.getElementById('linkPolitica');
    const modalPolitica = document.getElementById('modalPolitica');
    const cerrarPolitica = document.getElementById('cerrarPolitica');
    const aceptarPoliticaBtn = document.getElementById('aceptarPoliticaBtn');
    const checkPolitica = document.getElementById('aceptaPolitica');
    const btnRegistrar = document.getElementById('btnRegistrar');
    const mensajes = document.getElementById('mensajes');

    linkPolitica.addEventListener('click', function (e) {
        e.preventDefault();
        modalPolitica.classList.remove('d-none');
    });

    cerrarPolitica.addEventListener('click', function () {
        modalPolitica.classList.add('d-none');
    });

    aceptarPoliticaBtn.addEventListener('click', function () {
        checkPolitica.checked = true;
        modalPolitica.classList.add('d-none');
    });

    modalPolitica.addEventListener('click', function (e) {
        if (!modalPolitica.querySelector('.modal-content').contains(e.target)) {
            modalPolitica.classList.add('d-none');
        }
    });

    btnRegistrar.addEventListener('click', function (e) {
        if (!checkPolitica.checked) {
            e.stopImmediatePropagation();
            mensajes.innerHTML = '<div class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Debes aceptar la Política de Tratamiento de Datos Personales para continuar.</div>';
            mensajes.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }, true);

});
