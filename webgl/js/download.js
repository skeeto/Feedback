var Download = {
    show: function(url) {
        var $download = $('.download')
                .css('display', 'block')
                .animate({height: '110px'}, 500)
                .on('click', function() {
                    Download.hide();
                });
        $download.find('a').attr('href', url);
        $download.find('img').attr('src', url);
    },
    hide: function() {
        var $download = $('.download')
                .animate({height: '0px'}, 500, function() {
                   $download.css('display', 'none');
                });
    }
};
