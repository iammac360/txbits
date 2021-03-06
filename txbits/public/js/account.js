$(function(){

    var info_template = Handlebars.compile($("#acct-info-template").html());
    var totp_secret_template = Handlebars.compile($("#totp-secret-template").html());
    var turnofftfa_template = Handlebars.compile($("#turn-off-tfa-template").html());

    function reload(){
        API.user().success(function(data){
            var verificationText = "";
            switch (data.result.verification) {
                case -1: verificationText = "Identity verification pending"; break;
                case 0: verificationText = "Not verified"; break;
                default : verificationText = "Verified";
            }
            data.result.verification = verificationText;
            $('#acct-info').html(info_template(data.result));

            //TODO: internationalize javascript messages
            $('#turnoff-emails').click(function(e){
                API.turnoff_emails().success(function(){
                    $.pnotify({
                        title: 'Mailing list subscription',
                        text: 'Mailing list subscription turned off.',
                        styling: 'bootstrap',
                        type: 'success',
                        text_escape: true
                    });
                    reload();
                });
            });

            //TODO: internationalize javascript messages
            $('#turnon-emails').click(function(e){
                API.turnon_emails().success(function(){
                    $.pnotify({
                        title: 'Mailing list subscription',
                        text: 'Mailing list subscription turned on.',
                        styling: 'bootstrap',
                        type: 'success',
                        text_escape: true
                    });
                    reload();
                });
            });

            $('#turnon-tfa').click(function(e){
                API.gen_totp_secret().success(function(data){
                    function enable(e) {
                        var code = $("#tfa-enable-modal").find('.code').val();
                        //TODO: internationalize javascript messages
                        API.turnon_tfa(code).success(function(){
                            $.pnotify({
                                title: 'Two factor authentication',
                                text: 'Two factor authentication turned on.',
                                styling: 'bootstrap',
                                type: 'success',
                                text_escape: true
                            });
                            reload();
                            $("#tfa-enable-modal").modal('hide');
                        });
                        e.preventDefault();
                    }
                    $("#tfa-enable-modal .modal-body").html(totp_secret_template(data.result)).find('form').submit(enable);
                    $("#tfa-enable-qr").qrcode({render: "div", size: 200, text: data.result.otpauth});
                    $("#tfa-enable-modal").modal().find('.btn-primary').off("click").click(enable);
                });
                e.preventDefault();
            });

            //TODO: internationalize javascript messages
            $('#turnoff-tfa').click(function(e) {
                function disable(e) {
                    var code = $("#tfa-disable-modal").find('.code').val();
                    API.turnoff_tfa(code).success(function(){
                        $.pnotify({
                            title: 'Two factor authentication',
                            text: 'Two factor authentication turned off.',
                            styling: 'bootstrap',
                            type: 'success',
                            text_escape: true
                        });
                        reload();
                        $("#tfa-disable-modal").modal('hide');
                    });
                    e.preventDefault();
                }
                $("#tfa-disable-modal .modal-body").html(turnofftfa_template()).find('form').submit(disable);
                $("#tfa-disable-modal").modal().find('.btn-primary').off("click").click(disable);
                e.preventDefault();
            });
        });
    }
    reload();
});
