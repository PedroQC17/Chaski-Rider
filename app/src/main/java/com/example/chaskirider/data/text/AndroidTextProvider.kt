package com.example.chaskirider.data.text

import android.content.Context
import com.example.chaskirider.R
import com.example.chaskirider.domain.text.TextKey
import com.example.chaskirider.domain.text.TextProvider

class AndroidTextProvider(private val context: Context) : TextProvider {
    override fun get(key: TextKey): String = context.getString(when (key) {
        TextKey.TEXT_COMPLETA_EL_BANCO_Y_EL_TITULAR -> R.string.text_completa_el_banco_y_el_titular
        TextKey.TEXT_COMPLETA_TUS_NOMBRES_Y_APELLIDOS -> R.string.text_completa_tus_nombres_y_apellidos
        TextKey.TEXT_CONTRASENA_CONFIGURADA_YA_PUEDES_INGRESAR_CON_TU -> R.string.text_contrasena_configurada_ya_puedes_ingresar_con_tu
        TextKey.TEXT_CORREO_O_CONTRASENA_INCORRECTOS -> R.string.text_correo_o_contrasena_incorrectos
        TextKey.TEXT_DATOS_BANCARIOS_GUARDADOS -> R.string.text_datos_bancarios_guardados
        TextKey.TEXT_DEBES_ACEPTAR_LOS_TERMINOS_Y_LA_POLITICA -> R.string.text_debes_aceptar_los_terminos_y_la_politica
        TextKey.TEXT_EL_ARCHIVO_ESTA_VACIO -> R.string.text_el_archivo_esta_vacio
        TextKey.TEXT_EL_ARCHIVO_SUPERA_LOS_10_MB -> R.string.text_el_archivo_supera_los_10_mb
        TextKey.TEXT_EL_CCI_DEBE_TENER_20_DIGITOS -> R.string.text_el_cci_debe_tener_20_digitos
        TextKey.TEXT_EL_DNI_DEBE_TENER_8_DIGITOS -> R.string.text_el_dni_debe_tener_8_digitos
        TextKey.TEXT_ESTE_DNI_YA_ESTA_SIENDO_USADO -> R.string.text_este_dni_ya_esta_siendo_usado
        TextKey.TEXT_ESTE_NUMERO_YA_ESTA_VINCULADO_A_UNA -> R.string.text_este_numero_ya_esta_vinculado_a_una
        TextKey.TEXT_FALTA_DESPLEGAR_EL_SERVICIO_DE_REGISTRO_EN -> R.string.text_falta_desplegar_el_servicio_de_registro_en
        TextKey.TEXT_FIREBASE_NO_PERMITE_CONSULTAR_TU_PERFIL_REVISA -> R.string.text_firebase_no_permite_consultar_tu_perfil_revisa
        TextKey.TEXT_GOOGLE_NO_DEVOLVIO_UNA_CREDENCIAL_VALIDA -> R.string.text_google_no_devolvio_una_credencial_valida
        TextKey.TEXT_INGRESA_UN_CELULAR_VALIDO_CON_CODIGO_DE -> R.string.text_ingresa_un_celular_valido_con_codigo_de
        TextKey.TEXT_INGRESA_UN_CORREO_VALIDO -> R.string.text_ingresa_un_correo_valido
        TextKey.TEXT_INGRESA_UN_NUMERO_DE_CUENTA_O_CCI -> R.string.text_ingresa_un_numero_de_cuenta_o_cci
        TextKey.TEXT_LA_CONTRASENA_NO_CUMPLE_LA_POLITICA_DE -> R.string.text_la_contrasena_no_cumple_la_politica_de
        TextKey.TEXT_LA_CREDENCIAL_YA_PERTENECE_A_OTRA_CUENTA -> R.string.text_la_credencial_ya_pertenece_a_otra_cuenta
        TextKey.TEXT_LA_CUENTA_NO_TIENE_CORREO -> R.string.text_la_cuenta_no_tiene_correo
        TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR -> R.string.text_la_sesion_cambio_vuelve_a_ingresar
        TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA -> R.string.text_no_hay_una_sesion_activa
        TextKey.TEXT_NO_SE_PUDO_ABRIR_EL_ARCHIVO -> R.string.text_no_se_pudo_abrir_el_archivo
        TextKey.TEXT_NO_SE_PUDO_CARGAR_TU_PERFIL_DE -> R.string.text_no_se_pudo_cargar_tu_perfil_de
        TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION -> R.string.text_no_se_pudo_completar_la_operacion
        TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION_REINTENTA -> R.string.text_no_se_pudo_completar_la_operacion_reintenta
        TextKey.TEXT_NO_SE_PUDO_CONECTAR_CON_EL_SERVIDOR -> R.string.text_no_se_pudo_conectar_con_el_servidor
        TextKey.TEXT_NO_SE_PUDO_CONSULTAR_TU_PERFIL_EN -> R.string.text_no_se_pudo_consultar_tu_perfil_en
        TextKey.TEXT_NO_SE_PUDO_GUARDAR_LA_INFORMACION -> R.string.text_no_se_pudo_guardar_la_informacion
        TextKey.TEXT_NOTIFICACIONES_DE_CHASKI_RIDER -> R.string.text_notificaciones_de_chaski_rider
        TextKey.TEXT_POR_SEGURIDAD_CIERRA_SESION_Y_VUELVE_A -> R.string.text_por_seguridad_cierra_sesion_y_vuelve_a
        TextKey.TEXT_SELECCIONA_BICICLETA_MOTOCICLETA_O_AUTOMOVIL -> R.string.text_selecciona_bicicleta_motocicleta_o_automovil
        TextKey.TEXT_SELECCIONA_UN_PDF_JPG_O_PNG -> R.string.text_selecciona_un_pdf_jpg_o_png
        TextKey.TEXT_SELECCIONA_UN_VEHICULO -> R.string.text_selecciona_un_vehiculo
        TextKey.TEXT_SI_EL_CORREO_CORRESPONDE_A_UNA_CUENTA -> R.string.text_si_el_correo_corresponde_a_una_cuenta
        TextKey.TEXT_SOLO_LOS_REPARTIDORES_HABILITADOS_PUEDEN_ACTIVARSE -> R.string.text_solo_los_repartidores_habilitados_pueden_activarse
        TextKey.TEXT_SUBE_TODOS_LOS_DOCUMENTOS_OBLIGATORIOS_ANTES_DE -> R.string.text_sube_todos_los_documentos_obligatorios_antes_de
        TextKey.TEXT_USA_AL_MENOS_8_CARACTERES_Y_CONFIRMA -> R.string.text_usa_al_menos_8_caracteres_y_confirma
        TextKey.TEXT_VUELVE_A_SUBIR_ESTE_DOCUMENTO_PARA_CONSULTARLO -> R.string.text_vuelve_a_subir_este_documento_para_consultarlo
        TextKey.TEXT_YA_TIENES_UNA_CONTRASENA_CONFIGURADA_USA_LA -> R.string.text_ya_tienes_una_contrasena_configurada_usa_la
        TextKey.SERVER_ESTE_REGISTRO_NO_ADMITE_CAMBIOS_EN_SU_ESTADO_ACTUAL -> R.string.server_este_registro_no_admite_cambios_en_su_estado_actual
        TextKey.SERVER_REFERENCIA_DE_DOCUMENTO_NO_VALIDA -> R.string.server_referencia_de_documento_no_valida
        TextKey.SERVER_EL_CONTENIDO_NO_CORRESPONDE_A_UN_PDF_JPG_O -> R.string.server_el_contenido_no_corresponde_a_un_pdf_jpg_o
        TextKey.SERVER_NO_SE_PUDO_COMPROBAR_EL_ARCHIVO_VUELVE_A_SUBIRLO -> R.string.server_no_se_pudo_comprobar_el_archivo_vuelve_a_subirlo
        TextKey.SERVER_INICIA_SESION_PARA_CONTINUAR -> R.string.server_inicia_sesion_para_continuar
        TextKey.SERVER_ACCION_NO_VALIDA -> R.string.server_accion_no_valida
        TextKey.SERVER_SOLO_LOS_REPARTIDORES_HABILITADOS_PUEDEN_CAMBIAR_SU_DISPONIBILIDAD -> R.string.server_solo_los_repartidores_habilitados_pueden_cambiar_su_disponibilidad
        TextKey.SERVER_COMPLETA_PRIMERO_TUS_DATOS_PERSONALES -> R.string.server_completa_primero_tus_datos_personales
        TextKey.SERVER_VUELVE_AL_PASO_1_PARA_VALIDAR_TU_DNI_Y -> R.string.server_vuelve_al_paso_1_para_validar_tu_dni_y
        TextKey.SERVER_ACCESO_RESTRINGIDO -> R.string.server_acceso_restringido
        TextKey.SERVER_REVISION_NO_VALIDA -> R.string.server_revision_no_valida
        TextKey.SERVER_EL_REGISTRO_NO_ESTA_PENDIENTE -> R.string.server_el_registro_no_esta_pendiente
        TextKey.SERVER_INDICA_EL_MOTIVO_DE_CORRECCION -> R.string.server_indica_el_motivo_de_correccion
        TextKey.SERVER_INFORMACION_NO_VALIDA -> R.string.server_informacion_no_valida
        TextKey.SERVER_INGRESA_UNA_CUENTA_O_CCI -> R.string.server_ingresa_una_cuenta_o_cci
        TextKey.SERVER_SELECCIONA_UN_PDF_JPG_O_PNG_DE_HASTA_10 -> R.string.server_selecciona_un_pdf_jpg_o_png_de_hasta_10
    })
    override fun resolveError(message: String?, fallback: TextKey): String =
        TextKey.entries.firstOrNull { get(it) == message }?.let(::get) ?: get(fallback)
}
